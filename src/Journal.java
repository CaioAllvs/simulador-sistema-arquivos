import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Journal binario do tipo WAL. Cada registro tem tamanho, checksum e
 * dados codificados. Gravado e sincronizado ANTES de alterar a imagem.
 */
public class Journal {
    private static final int ASSINATURA = 0x4A524E32; // JRN2
    private final Path caminho;

    public Journal(Path caminho) throws IOException {
        this.caminho = caminho;
        Files.createDirectories(caminho.getParent());
        if (!Files.exists(caminho) || Files.size(caminho) == 0) {
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(caminho))) {
                out.writeInt(ASSINATURA);
            }
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(caminho))) {
            if (in.readInt() != ASSINATURA) {
                throw new IOException("Journal antigo ou invalido. Preserve o backup e inicie com uma pasta dados vazia.");
            }
        }
    }

    public void registrar(long sequencia, String comando, String... argumentos) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream dados = new DataOutputStream(bytes)) {
            dados.writeLong(sequencia);
            dados.writeUTF(comando);
            dados.writeInt(argumentos.length);
            for (String arg : argumentos) dados.writeUTF(arg);
        }
        byte[] original = bytes.toByteArray();
        byte[] codificado = BinaryCodec.transformar(original);
        ByteArrayOutputStream registro = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(registro)) {
            out.writeInt(codificado.length);
            out.writeInt(BinaryCodec.checksum(original));
            out.write(codificado);
        }
        try (FileChannel canal = FileChannel.open(caminho, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(registro.toByteArray());
            while (buffer.hasRemaining()) canal.write(buffer);
            canal.force(true);
        }
    }

    public List<Entrada> ler() throws IOException {
        List<Entrada> entradas = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(caminho))) {
            if (in.readInt() != ASSINATURA) throw new IOException("Cabecalho do journal invalido.");
            long ultima = 0;
            while (in.available() > 0) {
                if (in.available() < 8) throw new IOException("Registro incompleto no journal.");
                int tamanho = in.readInt();
                int checksum = in.readInt();
                if (tamanho < 0 || tamanho > 10_000_000 || tamanho > in.available())
                    throw new IOException("Registro incompleto ou invalido no journal.");
                byte[] original = BinaryCodec.transformar(in.readNBytes(tamanho));
                BinaryCodec.conferir(original, checksum);
                try (DataInputStream registro = new DataInputStream(new ByteArrayInputStream(original))) {
                    long sequencia = registro.readLong();
                    String comando = registro.readUTF();
                    int quantidade = registro.readInt();
                    if (quantidade < 0 || quantidade > 3) throw new IOException("Argumentos invalidos no journal.");
                    String[] args = new String[quantidade];
                    for (int i = 0; i < quantidade; i++) args[i] = registro.readUTF();
                    if (registro.available() != 0 || sequencia != ultima + 1)
                        throw new IOException("Sequencia ou registro inconsistente no journal.");
                    entradas.add(new Entrada(sequencia, comando, args));
                    ultima = sequencia;
                }
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
