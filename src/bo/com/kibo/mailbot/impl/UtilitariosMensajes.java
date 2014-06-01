/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author Olvinho
 */
public class UtilitariosMensajes {

    public static char SEPERADOR_PARAMETROS = '.';

    public static File reservarNombre(String nombre) throws IOException {
        File f = new File(nombre);
        int i = 0;
        while (f.exists()) {
            i++;
            f = new File(nombre + i);
        }
        f.createNewFile();
        return f;
    }

    public static void copiarArchivo(File archivoOriginal, File archivoDestino) throws FileNotFoundException, IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(archivoOriginal);
            os = new FileOutputStream(archivoDestino);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (is != null){
                is.close();
            }
            if (os != null){
                os.close();
            }
        }
    }
    
    public static File bajarPrimerAdjunto(Part parte) throws MessagingException, IOException{
        if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())){
            FileOutputStream fos = null;
            InputStream is = null;
            try {
                File salida = reservarNombre("temp\\" + parte.getFileName());
                is = parte.getInputStream();
                fos = new FileOutputStream(salida);
                byte[] buf = new byte[4096];
                int bytesRead;
                while((bytesRead = is.read(buf))!=-1) {
                    fos.write(buf, 0, bytesRead);
                }
                return salida;
            } catch (MessagingException | IOException e) {
                throw e;
            }finally{
              if (is != null){
                  is.close();
              }
              if (fos != null){
                  fos.close();
              }
            }
        }
        if (parte.isMimeType("multipart/*")){
            Multipart multiPartes = (MimeMultipart)parte.getContent();
            for(int i = 0; i < multiPartes.getCount() ; i++){
                File f = bajarPrimerAdjunto(multiPartes.getBodyPart(i));
                if (f != null)
                    return f;
            }
            return null;
        }
        return null;
    }

}
