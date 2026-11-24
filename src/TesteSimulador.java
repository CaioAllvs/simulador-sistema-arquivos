import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Teste simples sem bibliotecas externas: javac src/*.java && java -cp src TesteSimulador */
public class TesteSimulador {
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("fs-simulador-teste-");
        String segredo = "MENSAGEM_TESTE_12345";
        FileSystemSimulator fs = new FileSystemSimulator(pasta);
        fs.criarDiretorio("/docs");
        fs.criarArquivo("/docs/arquivo.txt", segredo);
        fs.copiarArquivo("/docs/arquivo.txt", "/docs/copia.txt");
        fs.renomearArquivo("/docs/copia.txt", "/docs/renomeado.txt");
        fs.apagarArquivo("/docs/arquivo.txt");
        fs.renomearDiretorio("/docs", "/arquivos");
        fs.listar("/arquivos");

        byte[] texto = segredo.getBytes(StandardCharsets.UTF_8);
        for (String nome : new String[]{"sistema.dat", "journal.log"}) {
            byte[] dados = Files.readAllBytes(pasta.resolve(nome));
            if (contem(dados, texto)) throw new AssertionError("Texto legivel em " + nome);
        }
        FileSystemSimulator reaberto = new FileSystemSimulator(pasta);
        reaberto.mostrarArquivo("/arquivos/renomeado.txt");

        // Reconstroi apenas pelo journal em pasta de teste descartavel.
        Files.delete(pasta.resolve("sistema.dat"));
        FileSystemSimulator recuperado = new FileSystemSimulator(pasta);
        recuperado.mostrarArquivo("/arquivos/renomeado.txt");
        recuperado.apagarArquivo("/arquivos/renomeado.txt");
        recuperado.apagarDiretorio("/arquivos");
        System.out.println("TESTES OK: operacoes, codificacao e recuperacao.");
    }

    private static boolean contem(byte[] dados, byte[] texto) {
        for (int i = 0; i <= dados.length - texto.length; i++) {
            if (Arrays.equals(Arrays.copyOfRange(dados, i, i + texto.length), texto)) return true;
        }
        return false;
    }
}
