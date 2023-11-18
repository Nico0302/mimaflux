/*
 * This file is part of the tool MimaFlux.
 * https://github.com/mattulbrich/mimaflux
 *
 * MimaFlux is a time travel debugger for the Minimal Machine
 * used in Informatics teaching at a number of schools.
 *
 * The system is protected by the GNU General Public License Version 3.
 * See the file LICENSE in the main directory of the project.
 *
 * (c) 2016-2022 Karlsruhe Institute of Technology
 *
 * Adapted for Mima by Mattias Ulbrich
 */
package edu.kit.kastel.formal.mimaflux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Timeline {

    public final static int STEP = -3;

    private final Update[][] updates;

    private final String fileContent;
    private final Map<String, Integer> labelMap;
    private final List<Command> commands;
    private final State state;

    private int currentPosition = 0;
    private List<UpdateListener> listeners = new ArrayList<>();

    public Timeline(Update[][] updates, String fileContent, Map<String, Integer> labelMap,
                    List<Command> commands, Map<Integer, Integer> initialValues) {
        this.updates = updates;
        this.fileContent = fileContent;
        this.labelMap = labelMap;
        this.commands = commands;
        this.state = new State(commands, initialValues);
        int start = labelMap.getOrDefault(Constants.START_LABEL, 0);
        state.set(State.IAR, start);
    }

    private void update(int addr, int val) {
        if(addr != STEP) {
            state.set(addr, val);
        }
        for (UpdateListener listener : listeners) {
            listener.memoryChanged(addr, val);
        }
    }

    public State exposeState() {
        return state;
    }

    public void addListener(UpdateListener listener) {
        listeners.add(listener);
    }

    public void addToPosition(int offset) {
        setPosition(currentPosition + offset);
    }

    public void setPosition(int position) {

        position = Math.min(updates.length, position);
        position = Math.max(0, position);

        if(currentPosition < position) {
            while(currentPosition < position) {
                incrementPosition();
            }
        } else {
            while (currentPosition > position) {
                decrementPosition();
            }
        }

        update(STEP, currentPosition);
    }

    private void decrementPosition() {
        currentPosition--;
        for (Update update : updates[currentPosition]) {
            update(update.addr(), update.oldValue());
        }
    }

    private void incrementPosition() {
        for (Update update : updates[currentPosition]) {
            update(update.addr(), update.newValue());
        }
        currentPosition ++;
    }

    public int getPosition() {
        return currentPosition;
    }

    public String getFileContent() {
        return fileContent;
    }

    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public int get(int adr) {
        return state.get(adr);
    }

    public int countStates() {
        return updates.length;
    }

    public Command findIARCommand() {
        for (Command command : commands) {
            if(command.address() == state.get(State.IAR)) {
                return command;
            }
        }
        return null;
    }

    public String getNameFor(int adr) {
        for (Entry<String, Integer> entry : labelMap.entrySet()) {
            if (entry.getValue() == adr) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<String, Integer> getLabelMap() {
        return labelMap;
    }
}
