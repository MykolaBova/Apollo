package apl.updater;

import java.util.Objects;

public class Version implements Comparable<Version> {
    private int majorVersion;
    private int intermediateVersion;
    private int minorVersion;

    public Version(int majorVersion, int intermediateVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.intermediateVersion = intermediateVersion;
        this.minorVersion = minorVersion;
    }

    public Version() {
    }

    @Override
    public String toString() {
        return majorVersion +"."+ intermediateVersion + "." + minorVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version)) return false;
        Version version = (Version) o;
        return majorVersion == version.majorVersion &&
                intermediateVersion == version.intermediateVersion &&
                minorVersion == version.minorVersion;
    }

    @Override
    public int hashCode() {

        return Objects.hash(majorVersion, intermediateVersion, minorVersion);
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getIntermediateVersion() {
        return intermediateVersion;
    }

    public void setIntermediateVersion(int intermediateVersion) {
        this.intermediateVersion = intermediateVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public static Version from(String versionString) {
        Objects.requireNonNull(versionString);
        if (!versionString.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new RuntimeException("Incorrect versionString :  " + versionString);
        }
        String[] versionNumbers = versionString.split("\\.");
        Version v = new Version();
        v.setMajorVersion(Integer.parseInt(versionNumbers[0]));
        v.setIntermediateVersion(Integer.parseInt(versionNumbers[1]));
        v.setMinorVersion(Integer.parseInt(versionNumbers[2]));
        return v;
    }

    @Override
    public int compareTo(Version v) {
        int majorVersionCompare = Integer.compare(majorVersion, v.getMajorVersion());
        if (majorVersionCompare != 0 )
            return majorVersionCompare;
        int intermediateVersionCompare = Integer.compare(intermediateVersion, v.getIntermediateVersion());
        if (intermediateVersionCompare != 0 )
            return majorVersionCompare;
        return Integer.compare(minorVersion, v.getMinorVersion());
    }

    public boolean greaterThan(Version v) {
        return compareTo(v) > 0;
    }

    public boolean lessThan(Version v) {
        return compareTo(v) < 0;
    }
}
