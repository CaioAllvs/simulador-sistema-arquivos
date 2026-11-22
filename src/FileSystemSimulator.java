import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/** Um disco virtual salvo em dados/sistema.dat e protegido pelo journal. */
public class FileSystemSimulator {
    private final Path imagem;
    private final Journal journal;
    private Directory raiz = new Directory("/");
    private long ultimaSequencia = 0;

    private static class Estado implements Serializable {
        private static final long serialVersionUID = 1L;
        Directory raiz;
        long ultimaSequencia;
        Estado(Directory raiz, long ultimaSequencia) {
            this.raiz = raiz;
            this.ultimaSequencia = ultimaSequencia;
        }
    }

    public FileSystemSimulator(Path pastaDados) throws IOException {
        Files.createDirectories(pastaDados);
        imagem = pastaDados.resolve("sistema.dat");
        journal = new Journal(pastaDados.resolve("journal.log"));
        if (Files.exists(imagem)) {
            try (ObjectInputStream entrada = new ObjectInputStream(Files.newInputStream(imagem))) {
                Estado estado = (Estado) entrada.readObject();
                raiz = estado.raiz;
                ultimaSequencia = estado.ultimaSequencia;
            } catch (ClassNotFoundException e) {
                throw new IOException("Nao foi possivel abrir o disco virtual.", e);
            }
        }
        // Se a execucao anterior parou entre o log e o salvamento, reaplica a operacao.
        boolean recuperou = false;
        for (Journal.Entrada entrada : journal.ler()) {
            if (entrada.sequencia > ultimaSequencia) {
                if (entrada.sequencia != ultimaSequencia + 1) {
                    throw new IOException("Sequencia inconsistente no journal.");
                }
                aplicar(entrada.comando, entrada.argumentos);
                ultimaSequencia = entrada.sequencia;
                recuperou = true;
            }
        }
        if (recuperou) salvar();
    }

    private String[] partes(String caminho) {
        if (caminho == null || !caminho.startsWith("/"))
            throw new IllegalArgumentException("Use caminho absoluto. Exemplo: /docs/texto.txt");
        if (caminho.equals("/")) return new String[0];
        String[] nomes = caminho.substring(1).split("/", -1);
        for (String nome : nomes) {
            if (nome.isBlank() || nome.equals(".") || nome.equals(".."))
                throw new IllegalArgumentException("Caminho invalido: " + caminho);
        }
        return nomes;
    }

    private Directory diretorio(String caminho) {
        Node atual = raiz;
        for (String parte : partes(caminho)) {
            if (!(atual instanceof Directory)) throw new IllegalArgumentException("Nao e diretorio: " + caminho);
            atual = ((Directory) atual).getItens().get(parte);
            if (atual == null) throw new IllegalArgumentException("Diretorio nao encontrado: " + caminho);
        }
        if (!(atual instanceof Directory)) throw new IllegalArgumentException("Nao e diretorio: " + caminho);
        return (Directory) atual;
    }

    private Directory pai(String caminho) {
        String[] nomes = partes(caminho);
        if (nomes.length == 0) throw new IllegalArgumentException("Nao e permitido modificar a raiz.");
        return diretorio(caminho.substring(0, caminho.lastIndexOf('/')).isEmpty()
                ? "/" : caminho.substring(0, caminho.lastIndexOf('/')));
    }

    private String nome(String caminho) {
        String[] nomes = partes(caminho);
        if (nomes.length == 0) throw new IllegalArgumentException("Caminho da raiz nao permitido.");
        return nomes[nomes.length - 1];
    }

    private void disponivel(String caminho) {
        if (pai(caminho).getItens().containsKey(nome(caminho)))
            throw new IllegalArgumentException("Ja existe: " + caminho);
    }

    private Node encontrado(String caminho, Class<?> tipo) {
        Node item = pai(caminho).getItens().get(nome(caminho));
        if (item == null || !tipo.isInstance(item))
            throw new IllegalArgumentException("Item nao encontrado ou tipo incorreto: " + caminho);
        return item;
    }

    private void executar(String comando, String... args) throws IOException {
        // Verifica a operacao antes de registra-la, evitando entradas invalidas no log.
        validar(comando, args);
        long proxima = ultimaSequencia + 1;
        journal.registrar(proxima, comando, args);
        aplicar(comando, args);
        ultimaSequencia = proxima;
        salvar();
    }

    private void validar(String comando, String[] a) {
        switch (comando) {
            case "MKDIR": case "TOUCH": disponivel(a[0]); break;
            case "CP": encontrado(a[0], File.class); disponivel(a[1]); break;
            case "RM": encontrado(a[0], File.class); break;
            case "RMDIR":
                Directory dir = (Directory) encontrado(a[0], Directory.class);
                if (!dir.getItens().isEmpty()) throw new IllegalArgumentException("O diretorio precisa estar vazio.");
                break;
            case "MVFILE": case "MVDIR":
                Class<?> tipo = comando.equals("MVFILE") ? File.class : Directory.class;
                encontrado(a[0], tipo);
                // Apenas renomear no mesmo diretorio, sem mover pastas.
                if (pai(a[0]) != pai(a[1])) throw new IllegalArgumentException("Use o mesmo diretorio ao renomear.");
                disponivel(a[1]);
                break;
            default: throw new IllegalArgumentException("Comando desconhecido: " + comando);
        }
    }

    private void aplicar(String comando, String[] a) {
        switch (comando) {
            case "MKDIR": pai(a[0]).getItens().put(nome(a[0]), new Directory(nome(a[0]))); break;
            case "TOUCH": pai(a[0]).getItens().put(nome(a[0]), new File(nome(a[0]), a[1])); break;
            case "CP":
                File original = (File) encontrado(a[0], File.class);
                pai(a[1]).getItens().put(nome(a[1]), new File(nome(a[1]), original.getConteudo()));
                break;
            case "RM": case "RMDIR": pai(a[0]).getItens().remove(nome(a[0])); break;
            case "MVFILE": case "MVDIR":
                Node item = pai(a[0]).getItens().remove(nome(a[0]));
                item.setNome(nome(a[1]));
                pai(a[1]).getItens().put(nome(a[1]), item);
                break;
            default: throw new IllegalArgumentException("Comando invalido no journal: " + comando);
        }
    }

    private void salvar() throws IOException {
        Path temporario = imagem.resolveSibling("sistema.tmp");
        try (ObjectOutputStream saida = new ObjectOutputStream(Files.newOutputStream(temporario))) {
            saida.writeObject(new Estado(raiz, ultimaSequencia));
        }
        try {
            Files.move(temporario, imagem, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(temporario, imagem, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void criarDiretorio(String caminho) throws IOException { executar("MKDIR", caminho); }
    public void criarArquivo(String caminho, String conteudo) throws IOException { executar("TOUCH", caminho, conteudo); }
    public void copiarArquivo(String origem, String destino) throws IOException { executar("CP", origem, destino); }
    public void apagarArquivo(String caminho) throws IOException { executar("RM", caminho); }
    public void renomearArquivo(String origem, String destino) throws IOException { executar("MVFILE", origem, destino); }
    public void apagarDiretorio(String caminho) throws IOException { executar("RMDIR", caminho); }
    public void renomearDiretorio(String origem, String destino) throws IOException { executar("MVDIR", origem, destino); }

    public void listar(String caminho) {
        Directory dir = diretorio(caminho);
        if (dir.getItens().isEmpty()) { System.out.println("  (vazio)"); return; }
        for (Map.Entry<String, Node> item : dir.getItens().entrySet()) {
            System.out.println("  " + (item.getValue() instanceof Directory ? "[DIR] " : "[ARQ] ") + item.getKey());
        }
    }

    public void mostrarArquivo(String caminho) {
        File file = (File) encontrado(caminho, File.class);
        System.out.println(file.getConteudo());
    }
}