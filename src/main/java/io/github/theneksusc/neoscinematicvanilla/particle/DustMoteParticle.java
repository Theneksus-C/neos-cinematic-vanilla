package io.github.theneksusc.neoscinematicvanilla.particle;

import io.github.theneksusc.neoscinematicvanilla.config.CinematicConfig;
import io.github.theneksusc.neoscinematicvanilla.config.ParticleSettings;
import io.github.theneksusc.neoscinematicvanilla.config.WindSettings;
import io.github.theneksusc.neoscinematicvanilla.wind.WindSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A mote of dust suspended in the air, drifting slowly and fading at both ends
 * of its life.
 *
 * <p>Three behaviours here are the reason this exists rather than a vanilla
 * particle:
 *
 * <ul>
 *   <li><b>Constant drift.</b> Gravity is zero and friction is one, so the
 *       velocity set at spawn never changes. Vanilla ash applies a friction of
 *       0.96 to a velocity it generates itself, which reads as a mote being
 *       flicked and then slowing to a stop.
 *   <li><b>Fading.</b> Opacity ramps up at the start of life and back down at
 *       the end, so motes are never seen to appear or vanish. No vanilla
 *       particle fades out.
 *   <li><b>Colour.</b> Set directly on the red, green, and blue fields, which
 *       the tintable vanilla particle only allows at the cost of a lifetime
 *       divided by its size.
 * </ul>
 *
 * <p>Rendering uses the translucent layer. On the opaque layer the alpha value
 * would be ignored and the fade would do nothing.
 *
 * <p>Every appearance value is read from config at spawn time rather than
 * captured as a constant, so an edited config affects motes created from that
 * moment on. Motes already in flight keep the values they were born with.
 */
public class DustMoteParticle extends SingleQuadParticle {

	/** Base rendered size, before the configured multiplier and random variation. */
	private static final float BASE_SIZE = 0.045F;
	private static final float SIZE_VARIATION = 0.035F;

	/** Fraction by which lifetime varies either side of the configured value. */
	private static final float LIFETIME_VARIATION = 0.35F;

	/** Fraction by which opacity varies either side of the configured value. */
	private static final float OPACITY_VARIATION = 0.25F;

	/** Base sideways drift per tick, before the configured multiplier. */
	private static final double BASE_HORIZONTAL_DRIFT = 0.0022;

	/** Base downward drift per tick, before the configured multiplier. */
	private static final double BASE_MIN_FALL_SPEED = 0.0012;
	private static final double BASE_FALL_SPEED_VARIATION = 0.0011;

	/** Shortest fade that still reads as a fade rather than a pop, in ticks. */
	private static final int MIN_FADE_TICKS = 1;

	/**
	 * Drift a mote reaches at full wind, in blocks per tick. A mote is light
	 * enough to be carried readily, so this dominates its own idle drift.
	 */
	private static final double WIND_DRIFT_SPEED = 0.025;

	/** Opacity this mote reaches at full fade in. */
	private final float peakAlpha;

	/** Length of this mote's fade at each end of its life, in ticks. */
	private final int fadeTicks;

	/**
	 * The mote's own idle drift, kept so wind can be added to it rather than
	 * accumulated into it. Writing wind into the velocity each tick would
	 * compound, since nothing here damps velocity.
	 */
	private final double baseXd;
	private final double baseZd;

	protected DustMoteParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			TextureAtlasSprite sprite,
			RandomSource random,
			ParticleSettings config,
			ColorParticleOption options) {

		super(level, x, y, z, sprite);

		this.setSize(0.01F, 0.01F);
		this.quadSize = (BASE_SIZE + random.nextFloat() * SIZE_VARIATION) * Math.max(0.0F, config.moteSize);

		int configuredLifetime = Math.max(1, Math.round(config.moteLifetimeSeconds * 20.0F));
		float lifetimeJitter = 1.0F + (random.nextFloat() * 2.0F - 1.0F) * LIFETIME_VARIATION;
		this.lifetime = Math.max(1, Math.round(configuredLifetime * lifetimeJitter));

		// Fade cannot exceed half the lifetime, or the two ends would overlap
		// and the mote would never reach full opacity.
		int configuredFade = Math.round(config.moteFadeSeconds * 20.0F);
		this.fadeTicks = Mth.clamp(configuredFade, MIN_FADE_TICKS, Math.max(MIN_FADE_TICKS, this.lifetime / 2));

		// Motes pass through blocks. Collision on something this small produces
		// visible sticking against surfaces rather than anything useful.
		this.hasPhysics = false;

		// Together these hold velocity constant for the whole life of the mote.
		this.friction = 1.0F;
		this.gravity = 0.0F;

		double drift = Math.max(0.0F, config.moteDriftSpeed);
		this.baseXd = (random.nextDouble() - 0.5) * BASE_HORIZONTAL_DRIFT * drift;
		this.baseZd = (random.nextDouble() - 0.5) * BASE_HORIZONTAL_DRIFT * drift;

		this.xd = this.baseXd;
		this.yd = -(BASE_MIN_FALL_SPEED + random.nextDouble() * BASE_FALL_SPEED_VARIATION) * drift;
		this.zd = this.baseZd;

		// Colour arrives with the spawn rather than from settings, because it
		// describes the place this mote is in rather than a preference.
		this.rCol = options.getRed();
		this.gCol = options.getGreen();
		this.bCol = options.getBlue();

		float opacityJitter = 1.0F + (random.nextFloat() * 2.0F - 1.0F) * OPACITY_VARIATION;
		this.peakAlpha = Mth.clamp(config.moteOpacity * opacityJitter, 0.0F, 1.0F);

		// Starts invisible so the first frame is the beginning of the fade in
		// rather than a mote appearing at full strength.
		this.alpha = 0.0F;
	}

	@Override
	public void tick() {
		applyWind();

		super.tick();

		if (this.removed) {
			return;
		}

		this.alpha = alphaForAge();
	}

	/**
	 * Sets velocity from the mote's own drift plus the current wind.
	 *
	 * <p>Assigned rather than accumulated. Velocity here is never damped, since
	 * friction is one, so adding wind every tick would compound into a mote
	 * shooting off. Assigning also means motes respond to a gust dying away,
	 * not just to it arriving.
	 */
	private void applyWind() {
		WindSettings wind = CinematicConfig.wind();

		if (!wind.enabled || wind.moteInfluence <= 0.0F) {
			this.xd = this.baseXd;
			this.zd = this.baseZd;
			return;
		}

		double scale = WIND_DRIFT_SPEED * wind.moteInfluence;

		this.xd = this.baseXd + WindSystem.driftX() * scale;
		this.zd = this.baseZd + WindSystem.driftZ() * scale;
	}

	/** Ramps opacity up at the start of life and back down at the end. */
	private float alphaForAge() {
		if (this.age < this.fadeTicks) {
			return this.peakAlpha * (this.age / (float) this.fadeTicks);
		}

		int remaining = this.lifetime - this.age;

		if (remaining < this.fadeTicks) {
			return this.peakAlpha * (remaining / (float) this.fadeTicks);
		}

		return this.peakAlpha;
	}

	@Override
	public SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	/** Creates motes using whatever the particle settings currently say. */
	public static class Provider implements ParticleProvider<ColorParticleOption> {

		private final SpriteSet sprites;

		public Provider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(
				ColorParticleOption options,
				ClientLevel level,
				double x,
				double y,
				double z,
				double xAux,
				double yAux,
				double zAux,
				RandomSource random) {

			return new DustMoteParticle(
					level, x, y, z, this.sprites.get(random), random, CinematicConfig.particles(), options);
		}
	}
}
