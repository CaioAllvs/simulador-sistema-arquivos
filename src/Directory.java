import java.util.Map;
import java.util.TreeMap;

// Diretorio virtual: guarda arquivos e outros diretorios.
public class Directory extends Node {
    private static final long serialVersionUID = 1L;
    private final Map<String, Node> itens = new TreeMap<>();

    public Directory(String nome) { super(nome); }
    public Map<String, Node> getItens() { return itens; }
}