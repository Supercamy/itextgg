
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;

import com.itextpdf.layout.element.Image;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import java.awt.image.BufferedImage;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.svg.converter.SvgConverter;


public class PdfGenerator {

    private static JFreeChart createBarChart(List<Map<String, Object>> records) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Map<String, Double> summedValues = new HashMap<>();
        for (Map<String, Object> record : records) {
            String use1 = (String) record.get("USE1");
            double arv = Double.parseDouble((String) record.get("ARV"));

            summedValues.put(use1, summedValues.getOrDefault(use1, 0.0) + arv);
        }

        List<Map.Entry<String, Double>> sortedEntries = summedValues.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        for (Map.Entry<String, Double> entry : sortedEntries) {
            dataset.addValue(entry.getValue() / 1_000_000, "ARV", entry.getKey());
        }

        JFreeChart barChart = ChartFactory.createBarChart(
                null,
                null,
                "ARV Building by Category (Millions)",
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                false
        );

        // Start of Styling
        CategoryPlot plot = barChart.getCategoryPlot();
        barChart.setBackgroundPaint(java.awt.Color.WHITE);
        plot.setBackgroundPaint(java.awt.Color.WHITE);


        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new java.awt.Color(0, 107, 162));
        renderer.setBarPainter(new StandardBarPainter());  // This disables the gradient effect

        Font helveticaFont = new Font("Helvetica", Font.PLAIN, 11);
        plot.getRangeAxis().setTickLabelFont(helveticaFont);
        plot.getDomainAxis().setTickLabelFont(helveticaFont);
        renderer.setDefaultItemLabelFont(helveticaFont);

        plot.setDomainGridlinesVisible(false);
        plot.setDomainGridlinePaint(java.awt.Color.LIGHT_GRAY);
        plot.setRangeGridlinesVisible(false);

        plot.setOutlineVisible(false);
        plot.getRangeAxis().setAxisLineVisible(false);
        plot.getRangeAxis().setTickMarksVisible(false);
        plot.getDomainAxis().setAxisLineVisible(false);
        plot.getDomainAxis().setTickMarksVisible(false);


        plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));

        NumberFormat format = NumberFormat.getIntegerInstance();
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}M", format));
        renderer.setDefaultItemLabelsVisible(true);

        // End of Styling

        return barChart;
    }

    private static Image convertChartToImage(JFreeChart chart, int width, int height) throws IOException {
        BufferedImage bufferedImage = chart.createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", os);
        byte[] byteArray = os.toByteArray();
        return new Image(ImageDataFactory.create(byteArray));
    }

    private static String convertChartToSVG(JFreeChart chart, int width, int height) throws IOException {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        chart.draw(svgGenerator, new java.awt.Rectangle(width, height));
        StringWriter svgWriter = new StringWriter();
        svgGenerator.stream(svgWriter, true);
        return svgWriter.toString();
    }

    public static class HeaderFooterEventHandler implements IEventHandler {
        private PdfFont font;
        Image leftImage;
        Image rightImage;

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM YY");

        public HeaderFooterEventHandler(Image leftImg, Image rightImg) {
            this.leftImage = leftImg;
            this.rightImage = rightImg;

            try {
                this.font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
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


            DeviceRgb headerFooterColor = new DeviceRgb(81, 36, 122);

            // Header
            pdfCanvas.saveState();
            pdfCanvas.setFillColor(headerFooterColor).rectangle(pageSize.getLeft(), pageSize.getTop() - 50, pageSize.getWidth(), 50).fill();
            pdfCanvas.restoreState();

            new Canvas(pdfCanvas, pageSize).add(leftImage.setFixedPosition(pageSize.getLeft() + 10, pageSize.getTop() - 40));
            new Canvas(pdfCanvas, pageSize).add(rightImage.setFixedPosition(pageSize.getRight() - 60, pageSize.getTop() - 40));
            pdfCanvas.beginText().setFontAndSize(font, 12).setColor(new DeviceRgb(255, 255, 255), true)
                    .moveText(pageSize.getWidth() / 2 - 50, pageSize.getTop() - 30)  // Approximate centering - adjust as needed
                    .showText("Building Summary")
                    .endText();

            // Footer
            pdfCanvas.saveState();
            pdfCanvas.setFillColor(headerFooterColor).rectangle(pageSize.getLeft(), pageSize.getBottom(), pageSize.getWidth(), 30).fill();
            pdfCanvas.restoreState();

            pdfCanvas.beginText().setFontAndSize(font, 8).setColor(new DeviceRgb(255, 255, 255), true)
                    .moveText(pageSize.getLeft() + 10, pageSize.getBottom() + 10)
                    .showText(String.format("Date: %s", dateFormat.format(new Date())))
                    .endText();

            pdfCanvas.beginText().setFontAndSize(font, 8).setColor(new DeviceRgb(255, 255, 255), true)
                    .moveText(pageSize.getRight() - 50, pageSize.getBottom() + 10)  // Adjust positioning for page number
                    .showText("Page: " + docEvent.getDocument().getPageNumber(page))
                    .endText();
        }
    }

    private static String formatARV(String arv) {

        int value = (int) Double.parseDouble(arv);
        if (value >= 1_000_000) {
            return String.format("$%sM", NumberFormat.getInstance().format(value / 1_000_000));
        } else if (value >= 1_000) {
            return String.format("$%sK", NumberFormat.getInstance().format(value / 1_000));
        } else {
            return String.format("$%s", NumberFormat.getInstance().format(value));
        }
    }

    private static String capitalize(String input) {
        String[] parts = input.split("\\W+"); // Split by non-word characters
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" "); // Add space between words
            result.append(Character.toUpperCase(parts[i].charAt(0)));
            result.append(parts[i].substring(1).toLowerCase());
        }

        String formattedResult = result.toString();

//        if ("Academic-Sei".equalsIgnoreCase(formattedResult)) {

        if ("Academic Sei".equalsIgnoreCase(formattedResult)) {
            return "Academic-SEI";
        } else if ("Academic Non Sei".equalsIgnoreCase(formattedResult)) {
            return "Academic Non-SEI";
        }

        return formattedResult;
    }

    public static void generatePdf(String path, List<Map<String, Object>> records) throws IOException, SQLException {
        PdfWriter pdfWriter = new PdfWriter(path);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);

        Image leftImage = new Image(ImageDataFactory.create("C:\\TMP\\itext\\UQlockup-Reverse-cmykpn.png"));
        Image rightImage = new Image(ImageDataFactory.create("C:\\TMP\\itext\\Logo-Right.png"));

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        HeaderFooterEventHandler eventHandler = new HeaderFooterEventHandler(leftImage, rightImage);
        pdfDocument.addEventHandler(PdfDocumentEvent.END_PAGE, eventHandler);
        float marginLeft = 36;     // Default
        float marginRight = 36;    // Default
        float marginTop = 70;     // Adjust this value based on your header height
        float marginBottom = 36;   // Default

        Document document = new Document(pdfDocument);
        document.setMargins(marginTop, marginRight, marginBottom, marginLeft);
        PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        Paragraph introParagraph = new Paragraph()
                .add("This report provides a comprehensive overview of buildings owned by UQ at site xxxx that have a recorded asset replacement value (ARV). ")
                .add("Detailed information, including images of these buildings, gross floor area (GFA), and primary organisational units, ")
                .add("is presented in the following sections.")
                .setFont(regularFont)
                .setFontSize(12)
                .setFontColor(DeviceRgb.BLACK)  // Setting font color to black
                .setMarginBottom(10);  // Add some space between the introduction and the chart


        document.add(introParagraph);

        Text bulletPoint = new Text("\u25A0 ")
                .setFont(regularFont)
                .setFontSize(12)
                .setFontColor(new DeviceRgb(128, 0, 128));

        int numberOfBuildings = records.size();


        Text buildingText = new Text("Number of buildings/structures " + numberOfBuildings)
                .setFont(regularFont)
                .setFontColor(DeviceRgb.BLACK)
                .setFontSize(12);


        Paragraph bulletParagraph = new Paragraph()
                .add(bulletPoint)
                .add(buildingText);

        document.add(bulletParagraph);

        JFreeChart barChart = createBarChart(records);
        String svgString = convertChartToSVG(barChart, 1000, 600);
        int svgWidth = 2000;  // You can adjust this value as needed
        int svgHeight = 1200;  // You can adjust this value as needed
        svgString = svgString.replaceFirst("<svg ", "<svg width=\"" + svgWidth + "\" height=\"" + svgHeight + "\" ");


        PdfFormXObject svgXObject = SvgConverter.convertToXObject(svgString, pdfDocument);
        Image svgImage = new Image(svgXObject);
        svgImage.scaleToFit(PageSize.A4.getWidth() - 100, PageSize.A4.getHeight() - 200);  // Adjust as needed
        svgImage.setFixedPosition(50, 100);  // Adjust as needed

        svgImage.scale(0.6f, 0.6f); // This will scale the image to 50% of its original size


        document.add(svgImage);

        document.add(new AreaBreak());

        int count = 0;

        float[] columnWidths = {400f, 200f};
        Table table = new Table(columnWidths);
        table.setWidth(PageSize.A4.getWidth() - marginLeft - marginRight);

        DeviceRgb blackColor = new DeviceRgb(0, 0, 0);  // RGB for black




        for (Map<String, Object> record : records) {
            int areaGrossInt = (int) Double.parseDouble((String) record.get("AREA_GROSS_INT"));  // Convert the value to integer.
            String formattedNumber = NumberFormat.getInstance().format(areaGrossInt);
            String formattedArea = String.format("%s m²", formattedNumber);
            String capitalizedUSE1 = capitalize((String) record.get("USE1"));

            String formattedARV = formatARV((String) record.get("ARV"));

            String textData = String.format("Building No: %s\n %s\nGFA: %s\nARV: %s\n %s",
                    record.get("BL_ID"), record.get("BLNAME"), formattedArea,
                    formattedARV, capitalizedUSE1);

            Paragraph cellParagraph = new Paragraph(textData).setFont(font).setFontColor(blackColor);

            Cell textCell = new Cell().add(cellParagraph);
            textCell.setBorder(Border.NO_BORDER);
            table.addCell(textCell);

            String bl_id = (String) record.get("BL_ID");
            File imageFile = new File("C:\\TMP\\itext\\" + bl_id + ".jpg");

            if (imageFile.exists()) {
                try {
                    Image image = new Image(ImageDataFactory.create(imageFile.getAbsolutePath()));
                    image.scaleAbsolute(180, 120); // Set the width and height as needed

                    Cell imageCell = new Cell();
                    imageCell.add(image);
                    imageCell.setBorder(Border.NO_BORDER);
                    table.addCell(imageCell);
                } catch (Exception e) {
                    System.out.println("Error processing image for BL_ID: " + bl_id);
                    e.printStackTrace();
                    table.addCell(new Cell().add(new Paragraph("Invalid Image")));
                }
            } else {
                Cell noImageCell = new Cell().add(new Paragraph("No Image Available"));
                noImageCell.setBorder(Border.NO_BORDER);
                table.addCell(noImageCell);
            }

            count++;
            if (count % 5 == 0) {
                document.add(table);
                table = new Table(columnWidths);
            }
        }

        // Ensure the last row of the table is complete.
        while (count % 2 != 0) {
            table.addCell(new Cell().add(new Paragraph(" ")));
            count++;
        }

        if (count % 5 != 0) {
            document.add(table);
            table = new Table(columnWidths);
        }
        document.close();

        System.out.println("Pdf Created");
    }
}
