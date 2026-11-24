import java.io.IOException;
import java.util.zip.CRC32;

/**
 * Codificacao reversivel para que a imagem e o journal nao exibam
 * textos diretamente em editores comuns. NAO e criptografia.
 */
public final class BinaryCodec {
    private BinaryCodec() {}

    public static byte[] transformar(byte[] dados) {
        byte[] saida = dados.clone();
        for (int i = 0; i < saida.length; i++) {
            saida[i] ^= (byte) (0xA7 + i * 31);
        }
        return saida;
    }

    public static int checksum(byte[] dados) {
        CRC32 crc = new CRC32();
        crc.update(dados);
        return (int) crc.getValue();
    }

    public static void conferir(byte[] dados, int esperado) throws IOException {
        if (checksum(dados) != esperado) {
            throw new IOException("Dados corrompidos: checksum invalido.");
        }
    }
}
