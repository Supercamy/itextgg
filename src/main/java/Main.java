import com.itextpdf.io.font.FontConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;

public class Main {
    public static void main(String[] args) throws IOException {

        String username = "pf_plann";
        String password = "uqchangeme";
        String connectionURL = "jdbc:oracle:thin:@(DESCRIPTION = " +
                "(ADDRESS = (PROTOCOL = TCP)(HOST = conman1.compute.dc.uq.edu.au)(PORT = 1521))" +
                "(ADDRESS = (PROTOCOL = TCP)(HOST = conman2.compute.dc.uq.edu.au)(PORT = 1521))" +
                "(CONNECT_DATA =(SERVICE_NAME = PFABPROD.SOE.UQ.EDU.AU)))";

        try {
            Connection connection = DriverManager.getConnection(connectionURL, username, password);
            System.out.println("Connected to Oracle database!");

            String query = "select site.name as STNAME, bl.site_id, bl.bl_id, bl.name as BLNAME, \n" +
                    "                         bl.area_gross_int, bl.status, map_bl.arv, bl.use1 ,\n" +
                    "                       CASE WHEN bl.bldg_photo IS NOT NULL THEN nvl(\n" +
                    "        (\n" +
                    "          SELECT d.file_contents\n" +
                    "          FROM\n" +
                    "            afm.afm_docvers d\n" +
                    "          WHERE\n" +
                    "              d.table_name = 'bl'\n" +
                    "            AND d.field_name = 'bldg_photo'\n" +
                    "            AND d.pkey_value = bl.bl_id\n" +
                    "            AND d.version = (\n" +
                    "                SELECT MAX(e.version)\n" +
                    "                FROM\n" +
                    "                  afm.afm_docvers e\n" +
                    "                WHERE\n" +
                    "                    e.table_name = 'bl'\n" +
                    "                  AND e.field_name = 'bldg_photo'\n" +
                    "                  AND e.pkey_value = bl.bl_id\n" +
                    "              )\n" +
                    "        ),\n" +
                    "        (\n" +
                    "          SELECT no_image\n" +
                    "          FROM\n" +
                    "            afm.v_icad_no_image\n" +
                    "        )\n" +
                    "      )\n" +
                    "      ELSE (\n" +
                    "        SELECT no_image\n" +
                    "        FROM\n" +
                    "          afm.v_icad_no_image\n" +
                    "      )\n" +
                    "    END\n" +
                    "  AS eq_photo\n" +
                    "                     from afm.bl\n" +
                    "                      join afm.map_bl on map_bl.bl_id = bl.bl_id\n" +
                    "                      join afm.site on site.site_id = bl.site_id\n" +
                    "\n" +
                    "                     where map_bl.arv != 0 and bl.site_id = '01'  order by bl.bl_id"; // Place your SQL query here.
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String bl_id = rs.getString("bl_id");
                Blob eq_photo_blob = rs.getBlob("eq_photo");

                // If the eq_photo column has data, save it to a file.
                if (eq_photo_blob != null) {
                    InputStream inputStream = eq_photo_blob.getBinaryStream();
                    FileOutputStream outputStream = new FileOutputStream("C:\\TMP\\itext\\" + bl_id + ".jpg");

                    byte[] buffer = new byte[4096];
                    int bytesRead = -1;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    inputStream.close();
                    outputStream.close();
                }
            }

            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        String path = "C:\\TMP\\itext\\FontPdf.pdf";
        PdfGenerator.generatePdf(path);
    }
}
