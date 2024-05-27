package br.com.dgconsult.reader_invoice.domain.service;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
// import com.google.zxing.client;

// import cn.luues.tool.qrcode.BufferedImageLuminanceSource;

public class PdfToImage {
    
    public void convertPdfToImage(String path){
        try {
             // Carregar o documento PDF
            File file = new File(path);
            PDDocument document = PDDocument.load(file);
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // Iterar sobre as páginas do documento
            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                // Renderizar a página como uma imagem
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 200, ImageType.RGB);

                // Salvar a imagem em um arquivo
                File output = new File("/boletos/imagens/imagem" + page + ".png");
                ImageIO.write(image, "png", output);
                codigoBarras();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void codigoBarras(){
        try {
            // Carregar a imagem que contém o código de barras
            File file = new File("/boletos/imagens/imagem3.png");
            BufferedImage image = ImageIO.read(file);

            // Converter a imagem para uma fonte de luminância
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            // Decodificar o código de barras
            Result result = new MultiFormatReader().decode(bitmap);

            // Imprimir o resultado
            System.out.println("Conteúdo do código de barras: " + result.getText());
        } catch (Exception e) {
            System.out.println("aqui");
            e.printStackTrace();
        }
    }
}
