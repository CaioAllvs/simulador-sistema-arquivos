import java.io.Serializable;

// Base para arquivos e diretorios do disco virtual.
public abstract class Node implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String nome;

    public Node(String nome) { this.nome = nome; }
    public String getNome() { return nome; }
    public void setNome(String novoNome) { this.nome = novoNome; }
}