package br.com.dgconsult.reader_invoice.domain.model;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoletoLinhaDigitavel {

    private String lojaOrigem;
	private String dataVencimento;
	private String valor;
	private String linhaDigitavel;    
    private String numeroDocumento;
    private String tipoBoleto = "Alugueis e outros";

    public void gerarTxt(String nomeArquivo, String path){
		try (FileWriter writer = new FileWriter(path + "/" + nomeArquivo)) {
            Class<?> classe = this.getClass();
            Field[] campos = classe.getDeclaredFields();

            for (Field campo : campos) {
                campo.setAccessible(true); // Permite acessar campos privados
                String nomeCampo = campo.getName();
                Object valorCampo = campo.get(this);
                String linha = nomeCampo + ": " + valorCampo + "\n";
                writer.write(linha);
            }
            System.out.println("Arquivo " + nomeArquivo + " gerado com sucesso!");
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
        }
	}

}
