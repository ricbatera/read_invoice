package br.com.dgconsult.reader_invoice.domain.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.dgconsult.reader_invoice.domain.model.Boleto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoletoBuilder {
    private String[] dados;
    private String typeBoleto;
    private Boleto boleto = new Boleto();

    public Boleto buildBoleto(String[] boletoData, String tipo) {
        dados = boletoData;
        typeBoleto = tipo;
        processaBoleto(tipo);
        return boleto;
    }

    private void processaBoleto(String tipo) {
        switch (tipo) {
            case "SABESP":
                sabesp();
                break;

            case "Multiplan":
                multiplan();
                break;

            case "CPFL":
                cpfl();
                break;

            case "Enel":
                enel();
                break;

            case "Saneago":
                saneago();
                break;

            case "Light":
                light();
                break;

            case "Edp":
                edp();
                break;

            case "Allos":
                allos();
                break;

            case "Airaz":
                airaz();
                break;

            default:
                break;
        }

    }

    private void sabesp() {
        boleto.setBeneficiario("SABESP");
        boleto.setDataVencimento(dados[29].split(" ")[1]);
        String valor = clearValues(dados[30]);
        boleto.setValor(valor);
        boleto.setCodBarras(dados[81]); // #pend2
        boleto.setTipoBoleto("Sabesp");
    }

    private void multiplan() {
        boleto.setBeneficiario(dados[11].substring(0, 29));
        boleto.setDataVencimento(findDateInString(dados[11]));
        String valor = clearValues(findValueInString(dados[56]));
        boleto.setValor(valor);
        boleto.setLinhaDigitavel(dados[24].substring(18)); // #pend2
        boleto.setTipoBoleto("Multiplan");
    }
    
    private void enel() {
        boleto.setBeneficiario("Enel");
        boleto.setCodBarras(dados[45].substring(0,51));
        String valor = findValueInString(dados[17].substring(20));
        boleto.setDataVencimento(findDateInString(dados[17]));
        boleto.setValor(clearValues(valor));
        boleto.setTipoBoleto("ENEL");
    }

    private void cpfl() {
        boleto.setBeneficiario("CPFL Energia");
        boleto.setCodBarras(processaCodBarrasCPFL(dados[60]));
        String valor = findValueInString(dados[52]);
        boleto.setDataVencimento(findDateInString(dados[52]));
        boleto.setValor(clearValues(valor));
        boleto.setTipoBoleto("CPFL");
    }

    private void saneago() {
        boleto.setBeneficiario("Saneamento de Goiás S.A.");
        boleto.setCodBarras(dados[62]);
        String valor = findValueInString(dados[17]);
        boleto.setDataVencimento(findDateInString(dados[61]));
        boleto.setValor(clearValues(valor));
        boleto.setTipoBoleto("Saneago");
    }

    private void light() {
        boleto.setBeneficiario("LIGHT SERVIÇOS DE ELETRICIDADE SA");
        boleto.setCodBarras(dados[55]);
        String valor = findValueInString(dados[19]);
        boleto.setDataVencimento(findDateInString(dados[54]));
        boleto.setValor(clearValues(valor));
        boleto.setTipoBoleto("Light");
    }

    private void edp() {
        boleto.setBeneficiario("EDP São Paulo Distribuição de Energia S.A");
        boleto.setCodBarras(dados[54].replaceAll("[^0-9 ]", "").trim());
        String valor = findValueInString(dados[51]);
        boleto.setDataVencimento(findDateInString(dados[51]));
        boleto.setValor(clearValues(valor));
        boleto.setTipoBoleto("Edp");
    }    

    private void allos() {
        boleto.setBeneficiario("BOULEVARD SHOPPING BELEM");
        boleto.setLinhaDigitavel(dados[21].substring(25));
        String valor = findValueInString(dados[29]);
        boleto.setDataVencimento(findDateInString(dados[24]));
        boleto.setValor(clearValues(valor));
        boleto.setTipoBoleto("Allos");
    }    

    private void airaz() {
        boleto.setBeneficiario("AIRAZ ADMINISTRADORA DE EMPREENDIMENTOS LTDA");
        boleto.setLinhaDigitavel(dados[27].substring(28));
        String valor = findValueInString(dados[24]);
        boleto.setDataVencimento(findDateInString(dados[10]));
        boleto.setValor(clearValues(valor));
        boleto.setTipoBoleto("Airaz");
    }    

    private String processaCodBarrasCPFL(String cod) {
        return cod.substring(0, 52);
        // .replace("O", "0")
        // .replace("G", "")
        // .replace("N", "0");

    }

    private String clearValues(String valor) {
        valor = valor.replaceAll("[^0-9]", "");
        if (valor.length() < 3) {
            return valor;
        }
        return valor.substring(0, valor.length() - 2) + "." + valor.substring(valor.length() - 2);
    }

    private String findValueInString(String data) {
        String padrao = "\\s*\\b(([1-9]\\d{0,2}(\\.\\d{3})*|0)(,\\d{2})|(99\\.000\\.000,00))\\b";
        // String padrao = "\\s*\\b(([1-9]\\d{0,2}(\\.\\d{3})*|0)(,\\d{2})?|(99\\.000\\.000,00))\\b";
        // String padrao =
        // "\\s*\\b(([1-9]\\d{0,2}(\\.\\d{3})*|0|\\d{1,2})(,\\d{2})?|(99\\.000\\.000,00))\\b";

        Pattern pattern = Pattern.compile(padrao);
        Matcher matcher = pattern.matcher(data);

        if (matcher.find()) {
            String parteEncontrada = matcher.group(1);
            System.out.println(parteEncontrada);
            return parteEncontrada;
        } else {
            return "Valor não encontrado";
        }
    }

    private String findDateInString(String data) {
        data = data.replace("/", ".");
        String padrao = "(\\d{2}\\.\\d{2}\\.\\d{4})";
        Pattern pattern = Pattern.compile(padrao);
        Matcher matcher = pattern.matcher(data);

        if (matcher.find()) {
            String parteEncontrada = matcher.group(1);
            parteEncontrada = convertUSADate(parteEncontrada);
            return parteEncontrada;
        } else {
            return "Data de vencimento não encontrada";
        }
    }

    private String convertUSADate(String data) {
        String novoPadrao = "yyyy-MM-dd";
        SimpleDateFormat formatoEntrada = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat formatoSaida = new SimpleDateFormat(novoPadrao);

        try {
            Date myData = formatoEntrada.parse(data);
            String dataConvertida = formatoSaida.format(myData);
            return dataConvertida;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
