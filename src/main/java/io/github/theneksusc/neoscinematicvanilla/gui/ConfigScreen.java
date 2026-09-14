package io.github.theneksusc.neoscinematicvanilla.gui;

import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.ConfigPresets;
import io.github.theneksusc.neoscinematicvanilla.config.FogSettings;
import io.github.theneksusc.neoscinematicvanilla.config.ParticleSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Settings screen for the mod.
 *
 * <p>Built from Minecraft's own option widgets rather than a configuration
 * library, so the mod ships as a single jar with nothing else to install. It
 * extends {@link OptionsSubScreen}, which supplies the scrolling list, the
 * header and footer layout, and the done button, leaving only the contents to
 * declare.
 *
 * <p>Values are written straight into the live config as each widget changes,
 * so the world behind the screen updates while a slider is being dragged. The
 * file is written once on close, rather than on every change, to avoid churning
 * the disk during a drag.
 *
 * <h2>Sliders hold integers</h2>
 *
 * <p>Minecraft's slider is integer based. A multiplier is therefore expressed
 * as an integer count of steps and mapped onto a decimal through {@code xmap},
 * which is the same approach vanilla uses for its own percentage options.
 */
public class ConfigScreen extends OptionsSubScreen {

	/** Steps per whole unit on a multiplier slider, giving 0.05 granularity. */
	private static final int STEPS_PER_UNIT = 20;

	/** Upper bound for multiplier sliders. Above 1 exaggerates an effect. */
	private static final double MAX_MULTIPLIER = 3.0;

	/** Build height limits, used to bound the elevation threshold sliders. */
	private static final int MIN_WORLD_Y = -64;
	private static final int MAX_WORLD_Y = 320;

	/** Footer button width, matching vanilla's own done button. */
	private static final int FOOTER_BUTTON_WIDTH = 150;

	public ConfigScreen(Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("neoscinematicvanilla.config.title"));
	}

	@Override
	protected void addOptions() {
		FogSettings fog = CinematicConfig.fog();
		ParticleSettings particles = CinematicConfig.particles();

		addPresets();
		addFogSection(fog);
		addParticleSection(particles);
		addMoteAppearanceSection(particles);
		addShaftSection(particles);
		addDebugSection(fog, particles);
	}

	/**
	 * Presets sit at the top because they are where a user starts, before
	 * refining anything below.
	 */
	private void addPresets() {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.presets"));

		ConfigPresets[] presets = ConfigPresets.values();

		// Two per row, matching how the option sliders below are laid out.
		for (int i = 0; i < presets.length; i += 2) {
			AbstractWidget left = presetButton(presets[i]);

			if (i + 1 < presets.length) {
				this.list.addSmall(left, presetButton(presets[i + 1]));
			} else {
				this.list.addBig(left);
			}
		}
	}

	private Button presetButton(ConfigPresets preset) {
		return Button.builder(Component.translatable(preset.translationKey()), button -> {
			preset.apply();

			// Rebuilding re-reads every value, so the sliders below jump to
			// what the preset just set rather than showing stale positions.
			this.rebuildWidgets();
		}).build();
	}

	private void addFogSection(FogSettings fog) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.fog"));

		this.list.addSmall(
				toggle("fog.enabled", fog.enabled, v -> fog.enabled = v),
				multiplier("fog.intensity", fog.intensity, v -> fog.intensity = v));

		this.list.addSmall(
				multiplier("fog.altitude", fog.altitudeInfluence, v -> fog.altitudeInfluence = v),
				multiplier("fog.cave", fog.caveInfluence, v -> fog.caveInfluence = v));

		this.list.addSmall(
				multiplier("fog.weather", fog.weatherInfluence, v -> fog.weatherInfluence = v),
				multiplier("fog.end_distance", fog.endDistance, v -> fog.endDistance = v));

		this.list.addSmall(
				multiplier("fog.start_distance", fog.startDistance, v -> fog.startDistance = v),
				ratio("fog.transition_speed", fog.transitionSpeed, v -> fog.transitionSpeed = v));

		this.list.addSmall(
				elevation("fog.haze_full_below", (int) fog.hazeFullBelowY, v -> fog.hazeFullBelowY = v),
				elevation("fog.haze_none_above", (int) fog.hazeNoneAboveY, v -> fog.hazeNoneAboveY = v));

		this.list.addSmall(
				elevation("fog.cave_full_below", (int) fog.caveFullBelowY, v -> fog.caveFullBelowY = v),
				elevation("fog.cave_none_above", (int) fog.caveNoneAboveY, v -> fog.caveNoneAboveY = v));

		this.list.addBig(toggle("fog.override_dimension", fog.overrideCustomDimensionFog,
				v -> fog.overrideCustomDimensionFog = v));
	}

	private void addParticleSection(ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.particles"));

		this.list.addSmall(
				toggle("particles.enabled", particles.enabled, v -> particles.enabled = v),
				multiplier("particles.density", particles.density, v -> particles.density = v));

		this.list.addSmall(
				multiplier("particles.cave", particles.caveDustDensity, v -> particles.caveDustDensity = v),
				multiplier("particles.forest", particles.forestMoteDensity, v -> particles.forestMoteDensity = v));

		this.list.addBig(multiplier("particles.jungle", particles.jungleRayDensity,
				v -> particles.jungleRayDensity = v));
	}

	private void addMoteAppearanceSection(ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.mote_appearance"));

		int rgb = particles.moteColorRgb();

		this.list.addSmall(
				colorChannel("particles.color_red", 16, (rgb >> 16) & 0xFF),
				colorChannel("particles.color_green", 8, (rgb >> 8) & 0xFF));

		this.list.addSmall(
				colorChannel("particles.color_blue", 0, rgb & 0xFF),
				ratio("particles.opacity", particles.moteOpacity, v -> particles.moteOpacity = v));

		this.list.addSmall(
				multiplier("particles.mote_size", particles.moteSize, v -> particles.moteSize = v),
				multiplier("particles.drift", particles.moteDriftSpeed, v -> particles.moteDriftSpeed = v));

		this.list.addSmall(
				seconds("particles.lifetime", 1, 60, Math.round(particles.moteLifetimeSeconds),
						v -> particles.moteLifetimeSeconds = v),
				seconds("particles.fade", 0, 10, Math.round(particles.moteFadeSeconds),
						v -> particles.moteFadeSeconds = v));
	}

	private void addShaftSection(ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.shafts"));

		this.list.addSmall(
				multiplier("particles.shaft_density", particles.shaftDensity, v -> particles.shaftDensity = v),
				multiplier("particles.shaft_length", particles.shaftLength, v -> particles.shaftLength = v));

		this.list.addSmall(
				multiplier("particles.shaft_radius", particles.shaftRadius, v -> particles.shaftRadius = v),
				multiplier("particles.shaft_lean", particles.shaftLean, v -> particles.shaftLean = v));
	}

	private void addDebugSection(FogSettings fog, ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.debug"));

		this.list.addSmall(
				toggle("fog.debug", fog.debugLogging, v -> fog.debugLogging = v),
				toggle("particles.debug", particles.debugLogging, v -> particles.debugLogging = v));
	}

	/** Adds a reset button beside vanilla's done button. */
	@Override
	protected void addFooter() {
		this.layout.addToFooter(Button.builder(
				Component.translatable("neoscinematicvanilla.button.reset"),
				button -> {
					CinematicConfig.resetToDefaults();
					this.rebuildWidgets();
				}).width(FOOTER_BUTTON_WIDTH).build());

		this.layout.addToFooter(Button.builder(
				CommonComponents.GUI_DONE,
				button -> this.onClose()).width(FOOTER_BUTTON_WIDTH).build());
	}

	/**
	 * Writes the config to disk once the screen closes.
	 *
	 * <p>Saving per change would rewrite the file dozens of times during a
	 * single slider drag. The live values are already applied, so nothing is
	 * lost by deferring the write.
	 */
	@Override
	public void removed() {
		CinematicConfig.save();
		super.removed();
	}

	/**
	 * Receives a slider value as a float.
	 *
	 * <p>The settings fields are floats while the slider works in doubles, and
	 * the standard {@code DoubleConsumer} would force a cast at every call site.
	 * One interface here keeps the option declarations readable.
	 */
	@FunctionalInterface
	private interface FloatSetter {
		void set(float value);
	}

	/** A 0 to 3 multiplier slider, shown to two decimal places. */
	private static OptionInstance<Double> multiplier(String key, float current, FloatSetter setter) {
		return decimal(key, MAX_MULTIPLIER, current, setter);
	}

	/** A 0 to 1 slider, for values that are genuinely a fraction. */
	private static OptionInstance<Double> ratio(String key, float current, FloatSetter setter) {
		return decimal(key, 1.0, current, setter);
	}

	/**
	 * A decimal slider backed by an integer one.
	 *
	 * <p>The initial value is clamped into range, because the config is a text
	 * file a user can put anything into, and a slider handed a value outside its
	 * bounds would otherwise render off its track.
	 */
	private static OptionInstance<Double> decimal(String key, double max, float current, FloatSetter setter) {
		int steps = (int) Math.round(max * STEPS_PER_UNIT);
		double clamped = Math.max(0.0, Math.min(max, current));

		return new OptionInstance<>(
				translationKey(key),
				OptionInstance.noTooltip(),
				(caption, value) -> valueLabel(caption, String.format("%.2f", value)),
				new OptionInstance.IntRange(0, steps)
						.xmap(v -> v / (double) STEPS_PER_UNIT, v -> (int) Math.round(v * STEPS_PER_UNIT), true),
				clamped,
				value -> setter.set((float) (double) value));
	}

	/** An elevation slider spanning the buildable world. */
	private static OptionInstance<Integer> elevation(String key, int current, IntConsumer setter) {
		return integerSlider(key, MIN_WORLD_Y, MAX_WORLD_Y, current, setter, String::valueOf);
	}

	/** A whole second slider. */
	private static OptionInstance<Integer> seconds(String key, int min, int max, int current, IntConsumer setter) {
		return integerSlider(key, min, max, current, setter, v -> v + "s");
	}

	/**
	 * A single colour channel. Reads the current colour, replaces one byte, and
	 * writes the whole value back, so the three channel sliders compose without
	 * needing to share state.
	 */
	private static OptionInstance<Integer> colorChannel(String key, int shift, int current) {
		return integerSlider(key, 0, 255, current, value -> {
			ParticleSettings particles = CinematicConfig.particles();
			int without = particles.moteColorRgb() & ~(0xFF << shift);
			particles.moteColor = String.format("#%06X", without | (value << shift));
		}, String::valueOf);
	}

	private static OptionInstance<Integer> integerSlider(
			String key,
			int min,
			int max,
			int current,
			IntConsumer setter,
			java.util.function.IntFunction<String> format) {

		return new OptionInstance<>(
				translationKey(key),
				OptionInstance.noTooltip(),
				(caption, value) -> valueLabel(caption, format.apply(value)),
				new OptionInstance.IntRange(min, max),
				Math.max(min, Math.min(max, current)),
				setter::accept);
	}

	private static OptionInstance<Boolean> toggle(String key, boolean current, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(translationKey(key), current, setter::accept);
	}

	private static String translationKey(String key) {
		return "neoscinematicvanilla.option." + key;
	}

	/** Renders as "Name: value", matching how vanilla labels its own sliders. */
	private static Component valueLabel(Component caption, String value) {
		return Component.translatable("options.generic_value", caption, value);
	}
}
