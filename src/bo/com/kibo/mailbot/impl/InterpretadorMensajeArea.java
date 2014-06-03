/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bo.com.kibo.mailbot.impl;

import bo.com.kibo.bl.exceptions.BusinessExceptionMessage;
import bo.com.kibo.bl.impl.control.FactoriaObjetosNegocio;
import bo.com.kibo.bl.intf.IAreaBO;
import bo.com.kibo.entidades.Area;
import bo.com.kibo.entidades.PuntoXY;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;

/**
 *
 * @author Olvinho
 */
public class InterpretadorMensajeArea extends InterpretadorMensajeGenerico<Area, Integer, IAreaBO> {

    @Override
    Area convertirHojaEnEntidad() {
        Area entidad = new Area();
        Cell celda;
        //Id
        celda = getCelda(3, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setId((int) celda.getNumericCellValue());
        }

        //Codigo
        celda = getCelda(4, 2);
        entidad.setCodigo(celda.getStringCellValue());

        //Año inicial
        celda = getCelda(5, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setAnioInicial((short) celda.getNumericCellValue());
        } else {
            appendException(new BusinessExceptionMessage("El año inicial debe ser un valor númerico entero"));
        }

        //Año final
        celda = getCelda(6, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setAnioFinal((short) celda.getNumericCellValue());
        } else {
            appendException(new BusinessExceptionMessage("El año final debe ser un valor númerico entero"));
        }

        //Zona UTM
        celda = getCelda(7, 2);
        if (celda.getCellType() == Cell.CELL_TYPE_NUMERIC) {
            entidad.setZonaUTM((byte) celda.getNumericCellValue());
        } else {
            appendException(new BusinessExceptionMessage("La zona debe tener un número válido"));
        }

        //Banda UTM
        celda = getCelda(8, 2);
        entidad.setBandaUTM(celda.getStringCellValue());
        //Poligono
        int i = 12;
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
                    appendException(new BusinessExceptionMessage("X, Y del polígono son requeridos y deben ser números", "", i - 11));
                }
            }
            i++;
        } while (celda.getCellType() != Cell.CELL_TYPE_BLANK);
        return entidad;
    }

    @Override
    IAreaBO getObjetoNegocio() {
        return FactoriaObjetosNegocio.getInstance().getAreaBO();
    }

    @Override
    boolean esNuevo(Area entidad) {
        return (entidad.getId() == null);
    }

    @Override
    String getId(Area entidad) {
        return entidad.getId().toString();
    }

    @Override
    Integer convertirId(String cadena) throws Exception {
        return convertirIdAEntero(cadena);
    }

    @Override
    void mostrarLista(List<Area> lista) {
        int i = 5;
        for (Area a : lista) {
            setValorCelda(i, 1, a.getId());
            setValorCelda(i, 2, a.getCodigo());
            setValorCelda(i, 3, a.getAnioInicial());
            setValorCelda(i, 4, a.getAnioFinal());
            setValorCelda(i, 5, a.getZonaUTM());
            setValorCelda(i, 6, a.getBandaUTM());
            i++;
        }
    }

    @Override
    void mostrarEntidad(Area entidad) {
        setValorCelda(3, 2, entidad.getId());
        setValorCelda(4, 2, entidad.getCodigo());
        setValorCelda(5, 2, entidad.getAnioInicial());
        setValorCelda(6, 2, entidad.getAnioFinal());
        setValorCelda(7, 2, entidad.getZonaUTM());
        setValorCelda(8, 2, entidad.getBandaUTM());
        int i = 12;
        for (PuntoXY punto : entidad.getPoligono()) {
            setValorCelda(i, 1, punto.getX());
            setValorCelda(i, 2, punto.getY());
            i++;
        }
    }

}
