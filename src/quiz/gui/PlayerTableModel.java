/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package quiz.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.table.AbstractTableModel;

import quiz.core.Player;

/**
 *
 * @author higor
 */
public final class PlayerTableModel extends AbstractTableModel {

    private String[] colNames = {"Nome", "Pronto"};//"Tempo", "Terminou"};
    private Class[] colTypes = {String.class, Boolean.class };//Integer.class, Boolean.class};
    private List<Player> players = new ArrayList<Player>();

    public PlayerTableModel(List<Player> players) {
        super();
        //this.players = players;
    }

    public PlayerTableModel() {
        super();
    }

    @Override
    public int getColumnCount() {
        return colNames.length;
    }

    @Override
    public int getRowCount() {
        if (players == null) {
            return 0;
        }
        return players.size();
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return colTypes[columnIndex];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex > players.size() - 1) {
            return null;
        }

        switch (columnIndex) {
            case 0:
                return players.get(rowIndex).getName();
            case 1:
                return players.get(rowIndex).isReady();
                //return players.get(rowIndex).getTime();
            //case 2:
//                return players.get(rowIndex).isDone();
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        if (rowIndex > players.size() - 1) {
            throw new IllegalArgumentException("No such row.");
        }

        switch (columnIndex) {
            case 0:
                players.get(rowIndex).setName((String) aValue);
                break;
            case 1:
                players.get(rowIndex) .setReady((Boolean) aValue);
//                players.get(rowIndex).setTime((Integer) aValue);
                break;
//            case 2:
//                players.get(rowIndex).setDone((Boolean) aValue);
//                break;
            default:
                throw new IllegalArgumentException("No such column.");
        }

        fireTableChanged(null);
    }

    public synchronized int size() {
        return players.size();
    }

    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }

    public synchronized void add(Player p) {
        if (players.contains(p)) {
            players.set(players.indexOf(p), p);
        } else {
            players.add(p);
        }

        fireTableChanged(null);
    }

    public synchronized void addAll(Collection<? extends Player> c) {
        players.addAll(c);
        fireTableChanged(null);
    }

    public synchronized void remove(Player p) {
        players.remove(p);
        fireTableChanged(null);
    }

    public synchronized void clear() {
        players.clear();
        fireTableChanged(null);
    }

    public synchronized boolean contains(Player p) {
        return players.contains(p);
    }

    public synchronized int indexOf(Player p) {
        return players.indexOf(p);
    }

    public synchronized Player find(String name) {
        for (Player p : players) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public void updatePlayers(Player player) {
        for (Player p : players) {
            p.update(player);
        }
        fireTableDataChanged();
    }
}
