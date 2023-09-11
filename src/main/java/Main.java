import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    private static final String OUTPUT_DIRECTORY = "C:\\TMP\\itext\\";
    public static void main(String[] args) throws IOException, SQLException {



        String username = "pf_plann";
        String password = "uqchangeme";
        String connectionURL = "jdbc:oracle:thin:@(DESCRIPTION = " +
                "(ADDRESS = (PROTOCOL = TCP)(HOST = conman1.compute.dc.uq.edu.au)(PORT = 1521))" +
                "(ADDRESS = (PROTOCOL = TCP)(HOST = conman2.compute.dc.uq.edu.au)(PORT = 1521))" +
                "(CONNECT_DATA =(SERVICE_NAME = PFABPROD.SOE.UQ.EDU.AU)))";

        List<Map<String, Object>> records = new ArrayList<>();


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
                    "                     where map_bl.arv != 0 and bl.site_id = '01'  order by bl.bl_id";


            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> record = new HashMap<>();
                record.put("BL_ID", rs.getString("BL_ID"));
                record.put("BLNAME", rs.getString("BLNAME"));
                record.put("AREA_GROSS_INT", rs.getString("AREA_GROSS_INT"));
                record.put("ARV", rs.getString("ARV"));
                record.put("USE1", rs.getString("USE1"));
                Blob eq_photo_blob = rs.getBlob("eq_photo");
//                byte[] bytes = null;
//                if (eq_photo_blob != null) {
//                    bytes = eq_photo_blob.getBytes(1, (int) eq_photo_blob.length());
//                }
                if (eq_photo_blob != null) {
                    byte[] bytes = eq_photo_blob.getBytes(1, (int) eq_photo_blob.length());
                    String imagePath = saveImage(bytes, rs.getString("BL_ID") + ".jpg");
                    record.put("eq_photo_path", imagePath);
                }
//                record.put("eq_photo_bytes", bytes);
                records.add(record);
            }

            rs.close();
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String path = OUTPUT_DIRECTORY + "FontPdf.pdf";
        PdfGenerator.generatePdf(path, records);
    }

    private static String saveImage(byte[] imageBytes, String fileName) throws IOException {
        Path outputPath = Paths.get(OUTPUT_DIRECTORY, fileName);
        Files.write(outputPath, imageBytes);
        return outputPath.toString();
    }

}
