package br.com.dgconsult.reader_invoice.domain.Enum;

public enum IdentificaBanco {
    ITAU(1, "341");

    private int banco;
    private String valorBanco;

    IdentificaBanco(int banco){
        this.banco = banco;
    }

    IdentificaBanco(int banco, String valorBanco) {
      this.valorBanco = valorBanco;
      this.banco = banco;
    }

    public int getBanco(){
        return this.banco;
    }

    public String getValorBanco(){
        return this.valorBanco;
    }
}
