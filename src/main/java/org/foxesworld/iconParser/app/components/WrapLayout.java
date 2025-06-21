package org.foxesworld.iconParser.app.components;

import java.awt.*;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 */
public class WrapLayout extends FlowLayout {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new WrapLayout with default alignment and gaps.
     */
    public WrapLayout() {
        super();
    }

    /**
     * Constructs a new WrapLayout with the specified alignment and default gaps.
     *
     * @param align The alignment value
     */
    public WrapLayout(int align) {
        super(align);
    }

    /**
     * Constructs a new WrapLayout with the specified alignment and gaps.
     *
     * @param align The alignment value
     * @param hgap The horizontal gap between components
     * @param vgap The vertical gap between components
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * Returns the preferred size of the container.
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    /**
     * Returns the minimum size of the container.
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    /**
     * Calculates the size of the container based on component sizes.
     *
     * @param target The container to calculate for
     * @param preferred true for preferred size, false for minimum size
     * @return The calculated dimension
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();
            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        dim.width = Math.max(dim.width, rowWidth);
                        dim.height += rowHeight + vgap;
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) {
                        rowWidth += hgap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            dim.width = Math.max(dim.width, rowWidth);
            dim.height += rowHeight + vgap;

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;

            return dim;
        }
    }
}