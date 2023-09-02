import com.itextpdf.io.font.FontConstants;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.ByteArrayInputStream;

public class PdfGenerator {

    public static void generatePdf(String path, List<Map<String, Object>> records) throws IOException, SQLException {
        PdfWriter pdfWriter = new PdfWriter(path);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        Document document = new Document(pdfDocument);

        PdfFont font = PdfFontFactory.createFont(FontConstants.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD);

        Text text1 = new Text("You are nice").setFont(font);
        Text text2 = new Text(" gordon").setFont(boldFont);

        Paragraph paragraph = new Paragraph().add(text1).add(text2);

        document.add(paragraph);

        int count = 0;
        Table table = new Table(2);

        for (Map<String, Object> record : records) {
            String textData = String.format("BL_ID: %s\nBLNAME: %s\nAREA_GROSS_INT: %s\nARV: %s\nUSE1: %s",
                    record.get("BL_ID"), record.get("BLNAME"), record.get("AREA_GROSS_INT"),
                    record.get("ARV"), record.get("USE1"));

            table.addCell(new Cell().add(new Paragraph(textData).setFont(font)));

            String bl_id = (String) record.get("BL_ID");
            File imageFile = new File("C:\\TMP\\itext\\" + bl_id + ".jpg");

            if (imageFile.exists()) {
                try {
                    Image image = new Image(ImageDataFactory.create(imageFile.getAbsolutePath()));
                    image.scaleToFit(200, 200);
                    Cell imageCell = new Cell();
                    imageCell.add(image);
                    table.addCell(imageCell);
                } catch (Exception e) {
                    System.out.println("Error processing image for BL_ID: " + bl_id);
                    e.printStackTrace();
                    table.addCell(new Cell().add(new Paragraph("Invalid Image")));
                }
            } else {
                table.addCell(new Cell().add(new Paragraph("No Image Available")));
            }


            count++;
            if (count % 5 == 0) {
                document.add(table);
                table = new Table(2); // Reset table
            }
        }

        // Ensure the last row of the table is complete.
        while (count % 2 != 0) {
            table.addCell(new Cell().add(new Paragraph(" ")));
            count++;
        }

        if (count % 5 != 0) {
            document.add(table);
        }
        document.close();

        System.out.println("Pdf Created");
    }
}
