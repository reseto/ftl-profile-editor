package net.blerf.ftl.parser.random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import net.blerf.ftl.parser.random.RandRNG;
import net.blerf.ftl.parser.random.NativeRandomJNI;


/**
 * This class calls srand()/rand() from platform-dependent C libraries.
 *
 * The RNG state is global for the entire process, so multiple instances will
 * interfere with each other. Instances on different OSs will yield different
 * random results for a given seed.
 */
public class NativeRandom implements RandRNG {
	// private static final Logger log = LoggerFactory.getLogger( NativeRandom.class );
	// private static final Logger log = NOPLogger.NOP_LOGGER;

	protected String name = null;

	NativeRandomJNI nativeInterface;

	public NativeRandom() {
		this( null );
		nativeInterface = new NativeRandomJNI();
	}

	public NativeRandom( String name ) {
		this.name = name;
		nativeInterface = new NativeRandomJNI();
	}

	@Override
	public void srand( int newSeed ) {
		nativeInterface.native_srand( newSeed );
	}

	static int count = 0;

	@Override
	public int rand() {
		int ret = nativeInterface.native_rand();
		// if (log.isDebugEnabled()) {
		// 	log.debug( String.format( "rng call %d: %d", count, ret ) );
		// 	count++;
		// }
		return ret;
	}

	@Override
	public void setName( String newName ) {
		name = newName;
	}

	@Override
	public String toString() {
		return (name != null ? name : super.toString());
	}
}
