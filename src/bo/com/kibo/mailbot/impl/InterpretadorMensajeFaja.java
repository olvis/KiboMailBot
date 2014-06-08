/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessExceptionMessage;
import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IFajaBO;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.entidades.Faja;
import bo.com.kibo.entidades.PuntoXY;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author Olvinho
 */
public class InterpretadorMensajeFaja extends InterpretadorMensajeGenerico<Faja, Integer, IFajaBO> {

    @Override
    Faja convertirHojaEnEntidad() {
        Faja entidad = new Faja();
        Cell celda;
        //Id
        celda = getCelda(3, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }
        //Bloque
        celda = getCelda(4, 2);
        entidad.setBloque(getValorCeldaCadena(celda));

        //Numero
        celda = getCelda(5, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setNumero((int) celda.getNumericCellValue());
        } else {
            appendException(new BusinessExceptionMessage("El número de faja debe ser un valor númerico entero"));
        }
        //Area
        entidad.setArea(new Area());
        celda = getCelda(6, 2);
        entidad.getArea().setCodigo(getValorCeldaCadena(celda));

        int i = 10;
        do {
            celda = getCelda(i, 1);
            Cell celdaY = getCelda(i, 2);
            if (celda.getCellType() != Cell.CELL_TYPE_BLANK) {
                if ((celda.getCellType() == Cell.CELL_TYPE_NUMERIC) && (celdaY.getCellType() == Cell.CELL_TYPE_NUMERIC)) {
                    PuntoXY punto = new PuntoXY();
                    punto.setX((float) celda.getNumericCellValue());
                    punto.setY((float) celdaY.getNumericCellValue());
                    entidad.getPoligono().add(punto);
                } else {
                    appendException(new BusinessExceptionMessage("X, Y del polígono son requeridos y deben ser números", "", i - 9));
                }
            }
            i++;
        } while (celda.getCellType() != Cell.CELL_TYPE_BLANK);

        return entidad;
    }

    @Override
    IFajaBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getFajaBO();
    }

    @Override
    boolean esNuevo(Faja entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(Faja entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<Faja> lista) {
        int i = 5;
        for (Faja f : lista) {
            setValorCelda(i, 1, f.getId());
            setValorCelda(i, 2, f.getBloque());
            setValorCelda(i, 3, f.getNumero());
            setValorCelda(i, 4, FactoriaObjetosNegocio.getInstance().getAreaBO().getCodigo(f.getArea().getId()));
            i++;
        }
    }

    @Override
    void mostrarEntidad(Faja entidad) {
        preparPlantillaAntesDeEnviar(); //Carguemos datos actualizados
        setValorCelda(3, 2, entidad.getId());
        setValorCelda(4, 2, entidad.getBloque());
        setValorCelda(5, 2, entidad.getNumero());
        setValorCelda(6, 2, entidad.getArea().getCodigo());
        int i = 10;
        for (PuntoXY punto : entidad.getPoligono()) {
            setValorCelda(i, 1, punto.getX());
            setValorCelda(i, 2, punto.getY());
            i++;
        }
    }

    @Override
    protected void preparPlantillaAntesDeEnviar() {
        List<Area> areas = FactoriaObjetosNegocio.getInstance().getAreaBO().obtenerTodos();
        String[] codigos = new String[areas.size()];
        for (int i = 0; i < areas.size(); i++) {
            codigos[i] = areas.get(i).getCodigo();
        }
        agregarValidacionLista(6, 6, 2, 2, codigos, true, true);
    }
}
