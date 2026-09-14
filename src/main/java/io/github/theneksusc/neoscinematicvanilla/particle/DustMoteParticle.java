package io.github.theneksusc.neoscinematicvanilla.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
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
 *   <li><b>Fading.</b> Opacity ramps up over the first {@link #FADE_TICKS} and
 *       back down over the last, so motes are never seen to appear or vanish.
 *       No vanilla particle fades out.
 *   <li><b>Colour.</b> Set directly on the red, green, and blue fields, which
 *       the tintable vanilla particle only allows at the cost of a lifetime
 *       divided by its size.
 * </ul>
 *
 * <p>Rendering uses the translucent layer. On the opaque layer the alpha value
 * would be ignored and the fade would do nothing.
 */
public class DustMoteParticle extends SingleQuadParticle {

	/** Length of the fade at each end of the particle's life, in ticks. */
	private static final int FADE_TICKS = 30;

	/** Opacity once fully faded in, before the per particle variation below. */
	private static final float BASE_ALPHA = 0.55F;
	private static final float ALPHA_VARIATION = 0.30F;

	/** Lifetime range in ticks. At 20 ticks per second this is 12 to 24 seconds. */
	private static final int MIN_LIFETIME = 240;
	private static final int LIFETIME_VARIATION = 240;

	/** Rendered size range. Deliberately small, so motes read as dust rather than debris. */
	private static final float MIN_SIZE = 0.045F;
	private static final float SIZE_VARIATION = 0.035F;

	/** Sideways drift, constant for the life of the mote. */
	private static final double HORIZONTAL_DRIFT = 0.0022;

	/** Downward drift. Slow enough that motes hang rather than fall. */
	private static final double MIN_FALL_SPEED = 0.0012;
	private static final double FALL_SPEED_VARIATION = 0.0011;

	/** Opacity this mote reaches at full fade in. */
	private final float peakAlpha;

	protected DustMoteParticle(
			ClientLevel level,
			double x,
			double y,
			double z,
			TextureAtlasSprite sprite,
			RandomSource random,
			float red,
			float green,
			float blue) {

		super(level, x, y, z, sprite);

		this.setSize(0.01F, 0.01F);
		this.quadSize = MIN_SIZE + random.nextFloat() * SIZE_VARIATION;
		this.lifetime = MIN_LIFETIME + random.nextInt(LIFETIME_VARIATION);

		// Motes pass through blocks. Collision on something this small produces
		// visible sticking against surfaces rather than anything useful.
		this.hasPhysics = false;

		// Together these hold velocity constant for the whole life of the mote.
		this.friction = 1.0F;
		this.gravity = 0.0F;

		this.xd = (random.nextDouble() - 0.5) * HORIZONTAL_DRIFT;
		this.yd = -(MIN_FALL_SPEED + random.nextDouble() * FALL_SPEED_VARIATION);
		this.zd = (random.nextDouble() - 0.5) * HORIZONTAL_DRIFT;

		this.rCol = red;
		this.gCol = green;
		this.bCol = blue;

		this.peakAlpha = BASE_ALPHA + random.nextFloat() * ALPHA_VARIATION;

		// Starts invisible so the first frame is the beginning of the fade in
		// rather than a mote appearing at full strength.
		this.alpha = 0.0F;
	}

	@Override
	public void tick() {
		super.tick();

		if (this.removed) {
			return;
		}

		this.alpha = alphaForAge();
	}

	/** Ramps opacity up at the start of life and back down at the end. */
	private float alphaForAge() {
		if (this.age < FADE_TICKS) {
			return this.peakAlpha * (this.age / (float) FADE_TICKS);
		}

		int remaining = this.lifetime - this.age;

		if (remaining < FADE_TICKS) {
			return this.peakAlpha * (remaining / (float) FADE_TICKS);
		}

		return this.peakAlpha;
	}

	@Override
	public SingleQuadParticle.Layer getLayer() {
		return SingleQuadParticle.Layer.TRANSLUCENT;
	}

	/**
	 * Creates motes in a warm yellow, suggesting dust catching sunlight.
	 *
	 * <p>Colour is fixed in the provider rather than carried on the particle
	 * options, which keeps the particle type simple. A second provider can be
	 * added later if a source needs a different tint.
	 */
	public static class SunlitProvider implements ParticleProvider<SimpleParticleType> {

		private static final float RED = 1.00F;
		private static final float GREEN = 0.90F;
		private static final float BLUE = 0.55F;

		private final SpriteSet sprites;

		public SunlitProvider(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(
				SimpleParticleType options,
				ClientLevel level,
				double x,
				double y,
				double z,
				double xAux,
				double yAux,
				double zAux,
				RandomSource random) {

			return new DustMoteParticle(level, x, y, z, this.sprites.get(random), random, RED, GREEN, BLUE);
		}
	}
}
