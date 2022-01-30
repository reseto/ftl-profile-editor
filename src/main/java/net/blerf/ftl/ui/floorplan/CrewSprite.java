package net.blerf.ftl.ui.floorplan;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import net.blerf.ftl.model.state.CrewState;
import net.blerf.ftl.ui.ReferenceSprite;
import net.blerf.ftl.ui.SpriteReference;

import static net.blerf.ftl.ui.UIConstants.SQUARE_SIZE;


public class CrewSprite extends JComponent implements ReferenceSprite<CrewState> {

    private BufferedImage crewImage = null;

    private final SpriteReference<CrewState> crewRef;
    private final SpriteImageProvider spriteImageProvider;


    public CrewSprite(SpriteReference<CrewState> crewRef, SpriteImageProvider spriteImageProvider) {
        this.crewRef = crewRef;
        this.spriteImageProvider = spriteImageProvider;

        this.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
        this.setOpaque(false);

        crewRef.addSprite(this);
        referenceChanged();
    }

    @Override
    public SpriteReference<CrewState> getReference() {
        return crewRef;
    }

    @Override
    public void referenceChanged() {
        crewImage = spriteImageProvider.getCrewBodyImage(crewRef.get().getRace(), crewRef.get().isMale(), crewRef.get().isPlayerControlled());

        this.repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(crewImage, 0, 0, this.getWidth(), this.getHeight(), this);
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %d HP)", crewRef.get().getName(), crewRef.get().getRace().getId(), crewRef.get().getHealth());
    }
}