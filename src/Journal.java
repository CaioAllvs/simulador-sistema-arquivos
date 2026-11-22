import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// O journal e escrito ANTES da modificacao: write-ahead logging (WAL).
public class Journal {
    private final Path caminho;

    public Journal(Path caminho) throws IOException {
        this.caminho = caminho;
        Files.createDirectories(caminho.getParent());
        if (!Files.exists(caminho)) Files.createFile(caminho);
    }

    public void registrar(long sequencia, String comando, String... argumentos) throws IOException {
        StringBuilder linha = new StringBuilder().append(sequencia).append('|').append(comando);
        for (String arg : argumentos) {
            linha.append('|').append(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(arg.getBytes(StandardCharsets.UTF_8)));
        }
        linha.append('\n');
        byte[] bytes = linha.toString().getBytes(StandardCharsets.UTF_8);
        try (FileChannel canal = FileChannel.open(caminho, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) canal.write(buffer);
            canal.force(true);
        }
    }

    public List<Entrada> ler() throws IOException {
        List<Entrada> entradas = new ArrayList<>();
        for (String linha : Files.readAllLines(caminho, StandardCharsets.UTF_8)) {
            if (linha.isBlank()) continue;
            String[] partes = linha.split("\\|", -1);
            try {
                long sequencia = Long.parseLong(partes[0]);
                String[] args = new String[partes.length - 2];
                for (int i = 2; i < partes.length; i++) {
                    args[i - 2] = new String(Base64.getUrlDecoder().decode(partes[i]), StandardCharsets.UTF_8);
                }
                entradas.add(new Entrada(sequencia, partes[1], args));
            } catch (RuntimeException erro) {
                throw new IOException("Journal invalido; confira o arquivo: " + linha, erro);
            }
        }
        return entradas;
    }

    public static class Entrada {
        public final long sequencia;
        public final String comando;
        public final String[] argumentos;

        public Entrada(long sequencia, String comando, String[] argumentos) {
            this.sequencia = sequencia;
            this.comando = comando;
            this.argumentos = argumentos;
        }
    }
}