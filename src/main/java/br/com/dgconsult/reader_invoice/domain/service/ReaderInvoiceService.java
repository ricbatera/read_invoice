package br.com.dgconsult.reader_invoice.domain.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.awt.image.BufferedImage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.dgconsult.reader_invoice.domain.model.Boleto;
import br.com.dgconsult.reader_invoice.domain.model.BoletoLinhaDigitavel;
import net.sourceforge.tess4j.Tesseract;

@Service
public class ReaderInvoiceService {
    String diretorio = "/boletos";
    String nomeArquivo = "erro.txt";
    String pathCompleto = "";
    Boolean successProcess = false;
    @Value("${tessdata.path}")
    private String tessdataPath;
    PdfToImage convert = new PdfToImage();
    String path = diretorio + "/processados";
    int paginas = 1;

    String linhaDigitavelCompleta;
    String dataVencimento;
    String valor;    
    String parte1 = "";
    String parte2 = "";

    public void contaBoletos() {
        printLogoConsult();

        System.out.println("Procuranto boletos em C:" + diretorio);

        try {
            List<Path> files = Files.list(Path.of(diretorio))
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .collect(Collectors.toList());

            System.out.println(files.size() + " boletos encontrados.");
            // files.forEach(boleto -> convert.convertPdfToImage(boleto.toString()));
            files.forEach(boleto -> readBoleto(boleto.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void readBoleto(String invoice) {
        System.out.println("\nProcessando o boleto: " + invoice);
        
        pathCompleto = invoice;
        PDDocument pdfDocument = null;
        successProcess = false;
        try {
            File file = new File(invoice);
            pdfDocument = PDDocument.load(file);
            paginas = pdfDocument.getNumberOfPages();
            StringBuilder textoCompleto = new StringBuilder();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int i = 1; i <= paginas; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String textoPagina = stripper.getText(pdfDocument);
                String res[] = textoPagina.toString().split("\r\n");
                String namePage = "_pag_"+i+".txt";
                nomeArquivo = invoice.replace(".pdf", namePage).replace("boletos\\", "");
                if (res.length > 0) {
                    // System.out.println(texto);
                    // processaBoleto(res);
                    findLinhaDigitável(res);
                }
                textoCompleto.append(textoPagina);
                
                // System.out.println(textoPagina);
            }
            String texto = textoCompleto.toString();
            String res[] = texto.split("\r\n");
          
            if (res.length == 0) {
                System.out.println("Iniciando análise profunda no pdf scaneado: " + invoice);
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath(tessdataPath);
                PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);
                for (int page = 0 ; page < paginas; page ++){
                    String namePage = "_pag_"+(page+1)+".txt";
                    nomeArquivo = invoice.replace(".pdf", namePage).replace("boletos\\", "");
                    BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
                    texto = tesseract.doOCR(bim);
                    res = texto.split("\n");
                    // System.out.println(texto);
                    findLinhaDigitável(res);
                }
                // processaBoleto(res);
            }
        } catch (InvalidPasswordException e) {
            System.out.println("Erro ao inserir a senha do boleto");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (pdfDocument != null) {
                try {
                    pdfDocument.close();
                    moverPdfProcessado(origemDestino(successProcess));
                } catch (IOException e) {
                    System.err.println("Erro ao mover arquivo: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }
    }



    private void processaBoleto(String[] dados) {
        // System.out.println("Tamano do Array: " + dados.length);
        Boleto boleto = new Boleto();
        BoletoBuilder builder = new BoletoBuilder();
        

        // BOLETOS SABESP #VALIDA BOLETO #TIPO: Sabesp
        if (dados.length > 83 && dados[83].contains("VIA SABESP")) {
            boleto = builder.buildBoleto(dados, "SABESP");
            boleto.gerarTxt(nomeArquivo, path);
            // System.out.println(boleto.toString()); // #APAGAR LOGS

            // BOLETOS MULTIPLAN #VALIDA BOLETO #TIPO: Multiplan
        } else if (dados.length > 11 && dados[11].contains("Multiplan Arrecadadora Ltda")) {
            boleto = builder.buildBoleto(dados, "Multiplan");
            boleto.gerarTxt(nomeArquivo, path);
            // System.out.println(boleto.toString()); // #APAGAR LOGS

            // BOLETOS CPFL #VALIDA BOLETO #TIPO: CPFL
        } else if (dados.length > 3 && dados[3].contains("Uma empresa do Grupo CPFL Energia")) {
            boleto = builder.buildBoleto(dados, "CPFL");
            boleto.gerarTxt(nomeArquivo, path);

            // BOLETOS ENEL #VALIDA BOLETO #TIPO: ENEL
        } else if (dados.length > 43 && dados[43].contains("ENEL DISTRIBUIÇÃO SÃO PAULO")) {
            boleto = builder.buildBoleto(dados, "Enel");
            boleto.gerarTxt(nomeArquivo, path);

            // BOLETOS ENEL #VALIDA BOLETO #TIPO: SANEAGO
        } else if (dados.length > 4 && dados[4].contains("SANEAGO")) {
            boleto = builder.buildBoleto(dados, "Saneago");
            boleto.gerarTxt(nomeArquivo, path);

            // BOLETOS ENEL #VALIDA BOLETO #TIPO: LIGHT
        } else if (dados.length > 4 && dados[1].contains("LIGHT SERVIÇOS DE ELETRICIDADE SA")) {
            boleto = builder.buildBoleto(dados, "Light");
            boleto.gerarTxt(nomeArquivo, path);

            // BOLETOS ENEL #VALIDA BOLETO #TIPO: EDP
        } else if (dados.length > 4 && dados[0].contains("EDP São Paulo Distribuição de Energia S.A")) {
            boleto = builder.buildBoleto(dados, "Edp");
            boleto.gerarTxt(nomeArquivo, path);

            // BOLETOS ENEL #VALIDA BOLETO #TIPO: ALLOS
        } else if (dados.length > 4 && dados[8].contains("BOULEVARD SHOPPING BELEM")) {
            boleto = builder.buildBoleto(dados, "Allos");
            boleto.gerarTxt(nomeArquivo, path);

            // BOLETOS ENEL #VALIDA BOLETO #TIPO: AIRAZ
        } else if (dados.length > 4 && dados[4].contains("AIRAZ")) {
            boleto = builder.buildBoleto(dados, "Airaz");
            boleto.gerarTxt(nomeArquivo, path);

        }

        successProcess = true;
    }

    private void findLinhaDigitável(String[] dados) {
        System.out.println("Processando boleto a procura de linha digitável");
        String linhaDigitavel = "";
        for (String linha : dados) {
            /*
             * 34191 - itaú
             * 2379 - bradesco
             * 13691 - unicred
             * 03399 - santander
             */
            if (linha.contains("34191") || linha.contains("2379") || linha.contains("13691") || linha.contains("03399")) {
                linhaDigitavel = linha;
                processaLinhaDigitavel(linhaDigitavel);
            }
            if(linha.contains("34191") || linha.contains("50164")){
                if(linha.contains("34191")){parte1 = linha;}
                if(linha.contains("50164")){ parte2 = linha;}
                if(parte1.length()>1 && parte2.length()>1){
                    processaExcessaoLinhaDigitavel();
                }
            }
        }
        paginas = paginas++;
        // System.out.println("Linha digitável: " + linhaDigitavel);
    }

    private void processaLinhaDigitavel(String linhaDigitavel) {
        // LIMPA A LINHA DIGITÁVEL REMOVENDO ESPAÇOES E PONTOS
        String res = linhaDigitavel.replace(".", "").replace(",", "").replace(")", "linhaDigitavel");
        res = res.replace(" ", "");

        // EXTRAI SOMENTE A PARTE DA DATA E VALOR DA LINHA DIGITÁVEL - VALIDA SE A STRING É TEM O TAMANNHO MINIMO DE UMA LINHA DIGITÁVEL
        if (res.length() >= 47) {
            linhaDigitavelCompleta = res.substring(res.length() -47);
            res = res.substring(res.length() - 14);

            // EXTRAI DA DATA DE VENCIMENTO DA LINHA DIGITÁVEL
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate initialDate = LocalDate.parse("1997-10-07", formatter);
            LocalDate newDate = initialDate.plusDays(Integer.parseInt(res.substring(0, 4)));
            dataVencimento = newDate.format(formatter);

            // REMOVE A PARTE DA DATA DA STRING E OS ZEROS À ESQUERDA DA LINHA DIGITÁVEL
            res = res.substring(res.length() - 10);
            res = res.replaceFirst("^0+", "");

            //FORMATA O VALOR COM "." PARA SEPARAR REAIS DE CENTAVOS
            String reais = res.substring(0, res.length() - 2);
            String centavos = res.substring(res.length() - 2);
            res = reais + "." + centavos;
            valor = res;
            criaTxtLinhaDigitavel();
            //System.out.println("\nValor: " + res + ". \nVencimento: " + newDate.format(formatter) + ". \nLinha Digitável: " + linhaCompleta + "\n\n");
        } else if(res.length() >= 24){ // abaixo trata uma execessão de um boleto que deu erro de lietura por causa da fonte e separou a linha digitável
            System.out.println("Testando possível execessão na leitura boleto");
            res = res.substring(res.length() - 14);

            // EXTRAI DA DATA DE VENCIMENTO DA LINHA DIGITÁVEL
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate initialDate = LocalDate.parse("1997-10-07", formatter);
            LocalDate newDate = initialDate.plusDays(Integer.parseInt(res.substring(0, 4)));
            dataVencimento = newDate.format(formatter);

            // REMOVE A PARTE DA DATA DA STRING E OS ZEROS À ESQUERDA DA LINHA DIGITÁVEL
            res = res.substring(res.length() - 10);
            res = res.replaceFirst("^0+", "");

            //FORMATA O VALOR COM "." PARA SEPARAR REAIS DE CENTAVOS
            String reais = res.substring(0, res.length() - 2);
            String centavos = res.substring(res.length() - 2);
            res = reais + "." + centavos;
            valor = res;
            processaExcessaoLinhaDigitavel();
            
        }
    }

    private void processaExcessaoLinhaDigitavel(){
        System.out.println(parte1);
        System.out.println(parte2);
        if(parte1.length() >1){
            String parteA = parte1.substring(0, parte1.length() - 17);
            parteA = parteA + parte2;
            if(parte2.length() > 1){
                String parteB = parte1.substring(parte1.length() - 17);
                parteA = parteA + parteB;
            }
            linhaDigitavelCompleta = parteA.replace(" ", "").replace(".", "").replace(",", "").replace(")", "linhaDigitavel");
            linhaDigitavelCompleta = linhaDigitavelCompleta.substring(linhaDigitavelCompleta.length() -47);
            criaTxtLinhaDigitavel();
        }
    }

    private void criaTxtLinhaDigitavel(){
        BoletoLinhaDigitavel boleto = new BoletoLinhaDigitavel();
        boleto.setDataVencimento(dataVencimento);
        boleto.setLinhaDigitavel(linhaDigitavelCompleta);
        boleto.setValor(valor);
        boleto.setLojaOrigem(nomeArquivo.substring(1, nomeArquivo.length()-4));
        boleto.gerarTxt(nomeArquivo, path);
        successProcess = true;
    }

    private void moverPdfProcessado(String[] path) throws IOException {
        Files.move(Paths.get(path[0]), Paths.get(path[1]), StandardCopyOption.REPLACE_EXISTING);
    }

    private String[] origemDestino(Boolean processOK) {
        String[] origemDestino = { "", "" };
        String processs = "processados/";
        String noProcess = "nao_processados/";
        int index = pathCompleto.lastIndexOf("\\");
        origemDestino[0] = pathCompleto.replace("\\", "/");

        if (index != -1) {
            // Dividir a string original em duas partes
            String parte1 = pathCompleto.substring(0, index + 1); // Incluindo a barra final
            String parte2 = pathCompleto.substring(index + 1);
            String resultado = "";
            // Concatenar as partes com o texto a ser inserido
            if (processOK) {
                resultado = parte1 + processs + parte2;
            } else {
                resultado = parte1 + noProcess + parte2;
            }
            origemDestino[1] = resultado.replace("\\", "/");
        } else {
            System.out.println("Ponto de inserção não encontrado na string original.");
        }
        return origemDestino;
    }

    private void printLogoConsult() {
        System.out.println(
                "\n\n            =++=-:                                                                                         \n"
                        +
                        "            +#####*+:                                                                                      \n"
                        +
                        "            :--==+*##=                                                                                     \n"
                        +
                        "                   :+*-                                              ..                                    \n"
                        +
                        "              :::    .+        =****=.                               +* .:   -****+-    .+****-            \n"
                        +
                        "           .=*##+     ..     .#+.  .=%:   ::.   . .:.    ::.  .    . +*.=*.. =#   .+#: =%-   :*+           \n"
                        +
                        "          .*=--::     .      *+        .+*==**..%*==#+ =#==*=-%   :% +*:*#=- =#     +#.%:                  \n"
                        +
                        "          :      ..:-==      #=        +*    =#.%.  :%.=#-:. -%   :% +* =*   =#     =%:%.  .***%-          \n"
                        +
                        "          .     :###*=       =#.    .+:++    =#.%.  .%. .-=*+:%   -% +* =*   =#    .#= #+     :%-          \n"
                        +
                        "          -:    .=-:.         -#*==+#+ .**--+#:.%.  .%.*+--*+ #*-=#%.+* =#=- =%===*#-  .+#+==**#-          \n"
                        +
                        "          .*+:                  .:::     .::.   :    :  .::.   ::: :... .::: .::::.       :::. :.          \n"
                        +
                        "           :*#*+=-::.                                                                                      \n"
                        +
                        "            .+*######:                                                                                     \n"
                        +
                        "              .-=+**#:                                                                                     \n");

    }

}
