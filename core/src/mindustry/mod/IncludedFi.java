package mindustry.mod;

import arc.Files;
import arc.files.Fi;
import net.jpountz.lz4.LZ4Factory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class IncludedFi extends Fi {
    private final String prefix;
    private final ClassLoader classLoader;

    public IncludedFi(ClassLoader classLoader, String prefix) {
        super(new File(""), Files.FileType.absolute);

        this.prefix = prefix;
        this.classLoader = classLoader;
    }

    @Override
    public boolean delete() {
        // NOOP. You can't delete a built-in.
        return true;
    }

    @Override
    public String path() {
        return prefix;
    }

    @Override
    public String name() {
        var parts = prefix.split("/");
        return parts[parts.length - 1];
    }

    @Override
    public Fi parent() {
        if (prefix.isEmpty()) return null;

        var lastSlash = prefix.lastIndexOf('/');
        if (lastSlash == -1) return new IncludedFi(classLoader, "");
        return new IncludedFi(classLoader, prefix.substring(0, lastSlash));
    }

    @Override
    public Fi child(String name) {
        return new IncludedFi(classLoader, prefix + "/" + name.replaceAll("/+$", ""));
    }

    @Override
    public Fi[] list() {
        // Stub. Not implemented.
        return new Fi[0];
    }

    @Override
    public boolean exists() {
        return classLoader.getResource(prefix) != null;
    }

    @Override
    public boolean isDirectory() {
        // Idk how to implement it properly so I don't bother.
        return classLoader.getResource(prefix) == null;
    }

    @Override
    public InputStream read() {
        return classLoader.getResourceAsStream(prefix);
    }

    @Override
    public long length() {
        try (var stream = classLoader.getResourceAsStream(prefix)) {
            long length = 0;

            var bytes = new byte[8192];

            while (true) {
                var l = stream.read(bytes);
                if (l == -1) break;
                length += l;
            }

            return length;
        } catch (IOException | NullPointerException e) {
            return 0;
        }
    }

    @Override
    public String toString() { return prefix; }
}
