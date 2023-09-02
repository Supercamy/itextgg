import com.itextpdf.io.font.FontConstants;
import com.itextpdf.io.image.ImageDataFactory;

import com.itextpdf.kernel.color.Color;
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
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;


import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;

import javax.swing.text.StyleConstants;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.ByteArrayInputStream;

public class PdfGenerator {
    public static class HeaderFooterEventHandler implements IEventHandler {
        private PdfFont font;
        Image leftImage;
        Image rightImage;



        public HeaderFooterEventHandler(Image leftImg, Image rightImg) {
            this.leftImage = leftImg;
            this.rightImage = rightImg;

            try {
                this.font = PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), docEvent.getDocument());
            leftImage.scale(0.07f, 0.07f);  // scales the image to 50% of its original size in both x and y dimensions
            rightImage.scale(0.06f, 0.06f);


            com.itextpdf.kernel.color.DeviceRgb headerFooterColor = new com.itextpdf.kernel.color.DeviceRgb(81, 36, 122);

            // Header
            pdfCanvas.saveState();
            pdfCanvas.setFillColor(headerFooterColor).rectangle(pageSize.getLeft(), pageSize.getTop() - 50, pageSize.getWidth(), 50).fill();
            pdfCanvas.restoreState();

            new Canvas(pdfCanvas, docEvent.getDocument(), pageSize).add(leftImage.setFixedPosition(pageSize.getLeft() + 10, pageSize.getTop() - 40));
            new Canvas(pdfCanvas, docEvent.getDocument(), pageSize).add(rightImage.setFixedPosition(pageSize.getRight() - 60, pageSize.getTop() - 40)); // Adjust positioning
            pdfCanvas.beginText().setFontAndSize(font, 12).setColor(Color.WHITE, true)
                    .moveText(pageSize.getWidth() / 2 - 50, pageSize.getTop() - 30)  // Approximate centering - adjust as needed
                    .showText("Building Summary")
                    .endText();

            // Footer
            pdfCanvas.saveState();
            pdfCanvas.setFillColor(headerFooterColor).rectangle(pageSize.getLeft(), pageSize.getBottom(), pageSize.getWidth(), 30).fill();
            pdfCanvas.restoreState();

            pdfCanvas.beginText().setFontAndSize(font, 12).setColor(Color.WHITE, true)
                    .moveText(pageSize.getLeft() + 10, pageSize.getBottom() + 10)
                    .showText(String.format("Date: %s", new Date()))
                    .endText();

            pdfCanvas.beginText().setFontAndSize(font, 12).setColor(Color.WHITE, true)
                    .moveText(pageSize.getRight() - 50, pageSize.getBottom() + 10)  // Adjust positioning for page number
                    .showText("Page: " + docEvent.getDocument().getPageNumber(page))
                    .endText();
        }
    }

    public static void generatePdf(String path, List<Map<String, Object>> records) throws IOException, SQLException {
        PdfWriter pdfWriter = new PdfWriter(path);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);

        Image leftImage = new Image(ImageDataFactory.create("C:\\TMP\\itext\\UQlockup-Reverse-cmykpn.png"));
        Image rightImage = new Image(ImageDataFactory.create("C:\\TMP\\itext\\Logo-Right.png"));

        PdfFont font = PdfFontFactory.createFont(FontConstants.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(FontConstants.HELVETICA_BOLD);

        HeaderFooterEventHandler eventHandler = new HeaderFooterEventHandler(leftImage, rightImage);
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, eventHandler);

        Document document = new Document(pdfDocument);

//        Text text1 = new Text("You are nice").setFont(font);
//        Text text2 = new Text(" gordon").setFont(boldFont);
//
//        Paragraph paragraph = new Paragraph().add(text1).add(text2);
//        document.add(paragraph);

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
