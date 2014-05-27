/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot;

/**
 *
 * @author Olvinho
 */
public class JavaMail implements Runnable {

    private boolean running;
    private IJavaMailListener listener;

    public JavaMail() {
        this.running = false;
    }

    public IJavaMailListener getListener() {
        return listener;
    }

    public void setListener(IJavaMailListener listener) {
        this.listener = listener;
    }

    private synchronized boolean isRunning() {
        return running;
    }

    private synchronized void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {
        while (isRunning()) {

        }
    }

    public void iniciar() {
        if (!isRunning()) {
            //Leer archivo de configuracion
            setRunning(true);
            Thread t = new Thread(this);
            t.start();
        }
    }

    public void parar() {
        if (isRunning()) {
            setRunning(false);
        }
    }

}
