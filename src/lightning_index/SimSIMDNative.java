package lightning_index;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * JNI wrapper for SimSIMD native library with zero-copy support
 */
public class SimSIMDNative {
    
    private static boolean nativeLoaded = false;
    
    static {
        try {
            // Try to load from classpath resources first (more portable)
            InputStream simsimdStream = SimSIMDNative.class.getResourceAsStream("/libsimsimd.dylib");
            InputStream jniStream = SimSIMDNative.class.getResourceAsStream("/libsimsimdjni.dylib");
            
            if (simsimdStream != null && jniStream != null) {
                File tempDir = Files.createTempDirectory("simsimd").toFile();
                tempDir.deleteOnExit();
                
                File tempSimsimd = new File(tempDir, "libsimsimd.dylib");
                File tempJni = new File(tempDir, "libsimsimdjni.dylib");
                
                Files.copy(simsimdStream, tempSimsimd.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(jniStream, tempJni.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                tempSimsimd.deleteOnExit();
                tempJni.deleteOnExit();
                
                System.load(tempSimsimd.getAbsolutePath());
                System.load(tempJni.getAbsolutePath());
                nativeLoaded = true;
                System.out.println("✅ SimSIMD native library loaded");
            }
        } catch (Exception e) {
            // Silent fallback
        }
        
        // Fallback: Try to load from resources directory
        if (!nativeLoaded) {
            try {
                String currentDir = System.getProperty("user.dir");
                String libPath = currentDir + "/resources/libsimsimdjni.dylib";
                String simsimdPath = currentDir + "/resources/libsimsimd.dylib";
                
                File libFile = new File(libPath);
                File simsimdFile = new File(simsimdPath);
                
                if (libFile.exists() && simsimdFile.exists()) {
                    // Load SimSIMD first
                    System.load(simsimdFile.getAbsolutePath());
                    // Then load our JNI wrapper
                    System.load(libFile.getAbsolutePath());
                    nativeLoaded = true;
                    System.out.println("✅ SimSIMD native library loaded");
                }
            } catch (UnsatisfiedLinkError e) {
                // Silent fallback
            }
        }
        
        if (!nativeLoaded) {
            System.err.println("⚠️ SimSIMD native library not found - using fallback Java implementation");
        }
    }
    
    // Standard SIMD methods
    public static native float euclideanF32(float[] a, float[] b, int dim);
    public static native float cosineF32(float[] a, float[] b, int dim);
    public static native void batchEuclideanF32(float[] query, float[][] vectors, float[] results, int dim);
    public static native void batchCosineF32(float[] query, float[][] vectors, float[] results, int dim);
    
    // Zero-copy methods - direct memory access
    public static native float euclideanF32Direct(long addr, float[] b, int dim);
    public static native float cosineF32Direct(long addr, float[] b, int dim);
    
    public static native String getCapabilities();
    
    public static boolean isNativeLoaded() {
        return nativeLoaded;
    }
    
    public static void printCapabilities() {
        if (nativeLoaded) {
            try {
                System.out.println(getCapabilities());
            } catch (Exception e) {
                System.out.println("Could not get capabilities: " + e.getMessage());
            }
        } else {
            System.out.println("Native library not loaded");
        }
    }
}
