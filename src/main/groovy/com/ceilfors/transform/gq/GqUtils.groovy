package com.ceilfors.transform.gq
/**
 * @author ceilfors
 */
class GqUtils {

    public static String TEMP_DIR = "GQTMP"

    public static void printToFile(Object value) {
        getGqFile().append("$value\n")
    }

    public static File getGqFile() {
        String directory
        if (System.getProperty(TEMP_DIR)) {
            directory = System.getProperty(TEMP_DIR)
        } else {
            if (System.getProperty("os.name").startsWith("Windows")) {
                // http://stackoverflow.com/questions/3282498/how-can-i-detect-a-unix-like-os-in-java
                directory = System.getProperty("java.io.tmpdir")
            } else {
                directory = "/tmp"
            }
        }
        return new File(directory, "gq")
    }
}
