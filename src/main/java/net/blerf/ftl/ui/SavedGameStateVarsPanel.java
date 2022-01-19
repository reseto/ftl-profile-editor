package net.blerf.ftl.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import net.blerf.ftl.parser.SavedGameParser;
import net.blerf.ftl.parser.SavedGameParser.StateVar;


public class SavedGameStateVarsPanel extends JPanel {

    private FTLFrame frame;
    private ArrayList<String> allVars = new ArrayList<String>();
    private FieldEditorPanel varsOnePanel = null;
    private FieldEditorPanel varsTwoPanel = null;


    public SavedGameStateVarsPanel(FTLFrame frame) {
        this.setLayout(new GridBagLayout());

        this.frame = frame;

        varsOnePanel = new FieldEditorPanel(true);
        varsOnePanel.setBorder(BorderFactory.createTitledBorder(""));
        varsOnePanel.addBlankRow();
        varsOnePanel.addFillRow();

        varsTwoPanel = new FieldEditorPanel(true);
        varsTwoPanel.setBorder(BorderFactory.createTitledBorder(""));
        varsTwoPanel.addBlankRow();
        varsTwoPanel.addFillRow();

        GridBagConstraints thisC = new GridBagConstraints();
        thisC.anchor = GridBagConstraints.NORTH;
        thisC.fill = GridBagConstraints.BOTH;
        thisC.weightx = 0.0;
        thisC.weighty = 0.0;
        thisC.gridx = 0;
        thisC.gridy = 0;
        this.add(varsOnePanel, thisC);

        thisC.gridx++;
        this.add(varsTwoPanel, thisC);

        thisC.fill = GridBagConstraints.BOTH;
        thisC.weighty = 1.0;
        thisC.gridx = 0;
        thisC.gridy++;
        this.add(Box.createVerticalGlue(), thisC);

        setGameState(null);
    }

    public void setGameState(SavedGameParser.SavedGameState gameState) {
        varsOnePanel.removeAll();
        varsTwoPanel.removeAll();

        if (gameState != null) {
            StateVar[] knownVars = StateVar.values();
            Map<String, Integer> savedVarMap = gameState.getStateVars();
            allVars = new ArrayList<String>();

            for (StateVar v : knownVars)
                allVars.add(v.getId());

            for (Map.Entry<String, Integer> entry : savedVarMap.entrySet()) {
                if (StateVar.findById(entry.getKey()) == null)
                    allVars.add(entry.getKey());
            }

            for (int i = 0; i < allVars.size(); i++) {
                String id = allVars.get(i);
                FieldEditorPanel tmpPanel = (i < (allVars.size() + 1) / 2 ? varsOnePanel : varsTwoPanel);
                tmpPanel.addRow(id, FieldEditorPanel.ContentType.INTEGER);
                if (gameState.hasStateVar(id)) {
                    tmpPanel.setIntAndReminder(id, gameState.getStateVar(id));
                }
                tmpPanel.getInt(id).addMouseListener(new StatusbarMouseListener(frame, StateVar.getDescription(id)));
            }
            varsOnePanel.addBlankRow();
            varsOnePanel.addFillRow();

            varsTwoPanel.addBlankRow();
            varsTwoPanel.addFillRow();
        }

        this.revalidate();
        this.repaint();
    }

    public void updateGameState(SavedGameParser.SavedGameState gameState) {
        String newString = null;
        gameState.getStateVars().clear();

        for (int i = 0; i < allVars.size(); i++) {
            String id = allVars.get(i);
            FieldEditorPanel tmpPanel = (i < (allVars.size() + 1) / 2) ? varsOnePanel : varsTwoPanel;

            newString = tmpPanel.getInt(id).getText();
            if (newString.length() > 0) {
                try {
                    int value = Integer.parseInt(newString);
                    gameState.setStateVar(id, value);
                } catch (NumberFormatException e) {
                }
            }
        }
    }
}
