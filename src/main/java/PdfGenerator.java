

import com.itextpdf.io.font.FontConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;

import java.io.IOException;

public class PdfGenerator {

    public static void generatePdf(String path) throws IOException {
        PdfWriter pdfWriter = new PdfWriter(path);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        Document document = new Document(pdfDocument);

        PdfFont font = PdfFontFactory.createFont(FontConstants.TIMES_ROMAN);
        PdfFont boldFont = PdfFontFactory.createFont(FontConstants.TIMES_BOLD);

        Text text1 = new Text("You are nice").setFont(font);
        Text text2 = new Text(" gordon").setFont(boldFont);

        Paragraph paragraph = new Paragraph().add(text1).add(text2);

        document.add(paragraph);
        document.close();

        System.out.println("Pdf Created");
    }
}
