package model;

public class Dipartimento {
	private Long codice;
	private String nome;
	
	public Dipartimento() {
	}
	
	public Dipartimento(String nome) {
		this.nome = nome;
	}
	
	public Long getCodice() {
		return codice;
	}
	public void setCodice(Long codice) {
		this.codice = codice;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public String toString() {
		return "Dipartimento[" + this.getCodice() + ", " + this.getNome()+"]"; 		
	}
	
	
}
