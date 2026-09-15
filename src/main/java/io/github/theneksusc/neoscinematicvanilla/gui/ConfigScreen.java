package io.github.theneksusc.neoscinematicvanilla.gui;

import io.github.theneksusc.neoscinematicvanilla.config.BiomeSettings;
import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.ConfigPresets;
import io.github.theneksusc.neoscinematicvanilla.config.FogSettings;
import io.github.theneksusc.neoscinematicvanilla.config.GrainSettings;
import io.github.theneksusc.neoscinematicvanilla.config.LeafSettings;
import io.github.theneksusc.neoscinematicvanilla.config.ParticleSettings;
import io.github.theneksusc.neoscinematicvanilla.config.WindSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
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

	/** Gap between the two footer buttons, matching vanilla's spacing. */
	private static final int FOOTER_BUTTON_SPACING = 8;

	public ConfigScreen(Screen lastScreen) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable("neoscinematicvanilla.config.title"));
	}

	/**
	 * Holds off config reloads while this screen is open.
	 *
	 * <p>A reload replaces the section objects and would discard edits made
	 * here. Editing in game takes precedence over the file until the screen
	 * closes, at which point the file is written from what was edited.
	 */
	@Override
	protected void init() {
		CinematicConfig.setReloadSuspended(true);
		super.init();
	}

	@Override
	protected void addOptions() {
		FogSettings fog = CinematicConfig.fog();
		ParticleSettings particles = CinematicConfig.particles();

		addPresets();
		addBiomeSection(CinematicConfig.biomes());
		addFogSection(fog);
		addParticleSection(particles);
		addMoteAppearanceSection(particles);
		addShaftSection(particles);
		addLeafSection(CinematicConfig.leaves());
		addWindSection(CinematicConfig.wind());
		addGrainSection();
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
			reopen();
		}).build();
	}

	/**
	 * Rebuilds the screen so every widget re-reads its value.
	 *
	 * <p>Replaces the screen rather than calling {@code rebuildWidgets}.
	 * {@link OptionsSubScreen} holds its layout in a final field initialised
	 * once, and its {@code init} adds to that layout on every call, so a rebuild
	 * stacks a second title, options list, and footer onto the first. The
	 * duplicated list backgrounds render over each other and darken the screen a
	 * little more with every press. {@code HeaderAndFooterLayout} offers no way
	 * to clear itself, so a fresh screen is the fix.
	 */
	private void reopen() {
		this.minecraft.setScreenAndShow(new ConfigScreen(this.lastScreen));
	}

	/**
	 * How strongly each kind of place differs from ordinary ground.
	 *
	 * <p>One control per character rather than per biome. These are classified
	 * from climate, so six cover every biome in the game and every modded one
	 * too, where sixty named sliders would cover vanilla and nothing else.
	 *
	 * <p>Each value is a distance from neutral, not a strength. At 0 that kind of
	 * place behaves like ordinary ground rather than losing its atmosphere.
	 */
	private void addBiomeSection(BiomeSettings biomes) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.biomes"));

		this.list.addSmall(
				toggle("biomes.enabled", biomes.enabled, v -> CinematicConfig.biomes().enabled = v),
				multiplier("biomes.strength", biomes.strength, v -> CinematicConfig.biomes().strength = v));

		this.list.addSmall(
				multiplier("biomes.arid", biomes.arid, v -> CinematicConfig.biomes().arid = v),
				multiplier("biomes.frozen", biomes.frozen, v -> CinematicConfig.biomes().frozen = v));

		this.list.addSmall(
				multiplier("biomes.swamp", biomes.swamp, v -> CinematicConfig.biomes().swamp = v),
				multiplier("biomes.wooded", biomes.wooded, v -> CinematicConfig.biomes().wooded = v));

		this.list.addBig(
				multiplier("biomes.highland", biomes.highland, v -> CinematicConfig.biomes().highland = v));
	}

	private void addFogSection(FogSettings fog) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.fog"));

		this.list.addSmall(
				toggle("fog.enabled", fog.enabled, v -> CinematicConfig.fog().enabled = v),
				multiplier("fog.intensity", fog.intensity, v -> CinematicConfig.fog().intensity = v));

		this.list.addSmall(
				multiplier("fog.altitude", fog.altitudeInfluence, v -> CinematicConfig.fog().altitudeInfluence = v),
				multiplier("fog.cave", fog.caveInfluence, v -> CinematicConfig.fog().caveInfluence = v));

		this.list.addSmall(
				multiplier("fog.weather", fog.weatherInfluence, v -> CinematicConfig.fog().weatherInfluence = v),
				multiplier("fog.end_distance", fog.endDistance, v -> CinematicConfig.fog().endDistance = v));

		this.list.addSmall(
				multiplier("fog.dawn_mist", fog.dawnMist, v -> CinematicConfig.fog().dawnMist = v),
				multiplier("fog.dusk_haze", fog.duskHaze, v -> CinematicConfig.fog().duskHaze = v));

		this.list.addBig(
				multiplier("fog.night_haze", fog.nightHaze, v -> CinematicConfig.fog().nightHaze = v));

		this.list.addSmall(
				multiplier("fog.start_distance", fog.startDistance, v -> CinematicConfig.fog().startDistance = v),
				ratio("fog.transition_speed", fog.transitionSpeed, v -> CinematicConfig.fog().transitionSpeed = v));

		this.list.addSmall(
				elevation("fog.haze_full_below", (int) fog.hazeFullBelowY, v -> CinematicConfig.fog().hazeFullBelowY = v),
				elevation("fog.haze_none_above", (int) fog.hazeNoneAboveY, v -> CinematicConfig.fog().hazeNoneAboveY = v));

		this.list.addSmall(
				elevation("fog.cave_full_below", (int) fog.caveFullBelowY, v -> CinematicConfig.fog().caveFullBelowY = v),
				elevation("fog.cave_none_above", (int) fog.caveNoneAboveY, v -> CinematicConfig.fog().caveNoneAboveY = v));

		this.list.addBig(toggle("fog.override_dimension", fog.overrideCustomDimensionFog,
				v -> CinematicConfig.fog().overrideCustomDimensionFog = v));
	}

	private void addParticleSection(ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.particles"));

		this.list.addSmall(
				toggle("particles.enabled", particles.enabled, v -> CinematicConfig.particles().enabled = v),
				multiplier("particles.density", particles.density, v -> CinematicConfig.particles().density = v));

		this.list.addSmall(
				multiplier("particles.cave", particles.caveDustDensity, v -> CinematicConfig.particles().caveDustDensity = v),
				multiplier("particles.surface", particles.surfaceMoteDensity,
						v -> CinematicConfig.particles().surfaceMoteDensity = v));
	}

	private void addMoteAppearanceSection(ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.mote_appearance"));

		// Colour is absent by design. It comes from the character of the place,
		// so a single chosen colour would flatten every biome back into one.
		this.list.addBig(
				ratio("particles.opacity", particles.moteOpacity, v -> CinematicConfig.particles().moteOpacity = v));

		this.list.addSmall(
				multiplier("particles.mote_size", particles.moteSize, v -> CinematicConfig.particles().moteSize = v),
				multiplier("particles.drift", particles.moteDriftSpeed, v -> CinematicConfig.particles().moteDriftSpeed = v));

		this.list.addSmall(
				seconds("particles.lifetime", 1, 60, Math.round(particles.moteLifetimeSeconds),
						v -> CinematicConfig.particles().moteLifetimeSeconds = v),
				seconds("particles.fade", 0, 10, Math.round(particles.moteFadeSeconds),
						v -> CinematicConfig.particles().moteFadeSeconds = v));
	}

	private void addShaftSection(ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.shafts"));

		this.list.addSmall(
				multiplier("particles.shaft_density", particles.shaftDensity, v -> CinematicConfig.particles().shaftDensity = v),
				multiplier("particles.shaft_length", particles.shaftLength, v -> CinematicConfig.particles().shaftLength = v));

		this.list.addSmall(
				multiplier("particles.shaft_radius", particles.shaftRadius, v -> CinematicConfig.particles().shaftRadius = v),
				multiplier("particles.shaft_lean", particles.shaftLean, v -> CinematicConfig.particles().shaftLean = v));
	}

	/**
	 * Leaves have only two settings because vanilla already handles the effect
	 * itself. This section scales how often it happens, nothing more.
	 */
	private void addLeafSection(LeafSettings leaves) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.leaves"));

		this.list.addSmall(
				toggle("leaves.enabled", leaves.enabled, v -> CinematicConfig.leaves().enabled = v),
				multiplier("leaves.frequency", leaves.frequency, v -> CinematicConfig.leaves().frequency = v));
	}

	/**
	 * Wind produces a direction and strength that the ambient loop, motes, and
	 * leaves all read, so its settings govern several effects at once rather
	 * than one of its own.
	 */
	private void addWindSection(WindSettings wind) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.wind"));

		this.list.addSmall(
				toggle("wind.enabled", wind.enabled, v -> CinematicConfig.wind().enabled = v),
				multiplier("wind.strength", wind.strength, v -> CinematicConfig.wind().strength = v));

		this.list.addSmall(
				ratio("wind.exposure", wind.exposureInfluence, v -> CinematicConfig.wind().exposureInfluence = v),
				multiplier("wind.weather", wind.weatherInfluence, v -> CinematicConfig.wind().weatherInfluence = v));

		this.list.addSmall(
				multiplier("wind.gustiness", wind.gustiness, v -> CinematicConfig.wind().gustiness = v),
				multiplier("wind.direction_speed", wind.directionChangeSpeed,
						v -> CinematicConfig.wind().directionChangeSpeed = v));

		this.list.addSmall(
				toggle("wind.sound_enabled", wind.soundEnabled, v -> CinematicConfig.wind().soundEnabled = v),
				ratio("wind.sound_volume", wind.soundVolume, v -> CinematicConfig.wind().soundVolume = v));

		this.list.addSmall(
				ratio("wind.sound_threshold", wind.soundThreshold, v -> CinematicConfig.wind().soundThreshold = v),
				multiplier("wind.mote_influence", wind.moteInfluence,
						v -> CinematicConfig.wind().moteInfluence = v));

		this.list.addSmall(
				multiplier("wind.leaf_influence", wind.leafInfluence, v -> CinematicConfig.wind().leafInfluence = v),
				toggle("wind.debug", wind.debugLogging, v -> CinematicConfig.wind().debugLogging = v));
	}

	/**
	 * Grain is a single stepped setting rather than a slider, because a post
	 * effect's uniforms are baked when it is built and one effect file ships per
	 * level. Naming the steps is clearer than a slider that silently snaps.
	 */
	private void addGrainSection() {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.grain"));

		this.list.addBig(integerSlider(
				"grain.level",
				GrainSettings.LEVEL_OFF,
				GrainSettings.LEVEL_MAX,
				CinematicConfig.grain().level,
				v -> CinematicConfig.grain().level = v,
				ConfigScreen::grainLevelName));
	}

	/** Names the grain steps, since the raw numbers would mean nothing to a reader. */
	private static String grainLevelName(int level) {
		return switch (level) {
			case 1 -> Component.translatable("neoscinematicvanilla.grain.light").getString();
			case 2 -> Component.translatable("neoscinematicvanilla.grain.medium").getString();
			case 3 -> Component.translatable("neoscinematicvanilla.grain.heavy").getString();
			default -> CommonComponents.OPTION_OFF.getString();
		};
	}

	private void addDebugSection(FogSettings fog, ParticleSettings particles) {
		this.list.addHeader(Component.translatable("neoscinematicvanilla.section.debug"));

		this.list.addSmall(
				toggle("fog.debug", fog.debugLogging, v -> CinematicConfig.fog().debugLogging = v),
				toggle("particles.debug", particles.debugLogging, v -> CinematicConfig.particles().debugLogging = v));
	}

	/**
	 * Adds a reset button beside vanilla's done button.
	 *
	 * <p>Both go inside a horizontal layout. The footer holds one element, so
	 * adding two buttons to it directly places them at the same position, one
	 * on top of the other. Only the upper one is visible, while the one added
	 * first receives the clicks, which makes the visible button silently do the
	 * wrong thing.
	 */
	@Override
	protected void addFooter() {
		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(FOOTER_BUTTON_SPACING));

		footer.addChild(Button.builder(
				Component.translatable("neoscinematicvanilla.button.reset"),
				button -> {
					CinematicConfig.resetToDefaults();
					reopen();
				}).width(FOOTER_BUTTON_WIDTH).build());

		footer.addChild(Button.builder(
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

		// Reopening constructs a new screen, whose init suspends again before
		// anything can reload, so lifting it here is safe.
		CinematicConfig.setReloadSuspended(false);

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
				tooltipFor(key),
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

	private static OptionInstance<Integer> integerSlider(
			String key,
			int min,
			int max,
			int current,
			IntConsumer setter,
			java.util.function.IntFunction<String> format) {

		return new OptionInstance<>(
				translationKey(key),
				tooltipFor(key),
				(caption, value) -> valueLabel(caption, format.apply(value)),
				new OptionInstance.IntRange(min, max),
				Math.max(min, Math.min(max, current)),
				setter::accept);
	}

	private static OptionInstance<Boolean> toggle(String key, boolean current, Consumer<Boolean> setter) {
		return OptionInstance.createBoolean(translationKey(key), tooltipFor(key), current, setter::accept);
	}

	/**
	 * Builds the tooltip for an option from its own key.
	 *
	 * <p>Derived rather than passed, so an option cannot be added without its
	 * explanation. A missing entry shows the raw key on screen, which is ugly
	 * enough to be noticed immediately rather than quietly shipping.
	 *
	 * <p>Cached, since the tooltip is rebuilt whenever the value changes and
	 * these are constant text.
	 */
	private static <T> OptionInstance.TooltipSupplier<T> tooltipFor(String key) {
		return OptionInstance.cachedConstantTooltip(
				Component.translatable(translationKey(key) + ".tooltip"));
	}

	private static String translationKey(String key) {
		return "neoscinematicvanilla.option." + key;
	}

	/** Renders as "Name: value", matching how vanilla labels its own sliders. */
	private static Component valueLabel(Component caption, String value) {
		return Component.translatable("options.generic_value", caption, value);
	}
}
