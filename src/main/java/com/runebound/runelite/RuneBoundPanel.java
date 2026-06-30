package com.runebound.runelite;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.time.Duration;
import java.time.Instant;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class RuneBoundPanel extends PluginPanel
{
	private static final long serialVersionUID = 1L;
	private static final String PRIVACY_NOTE = "View cached RuneBound profile summaries.";
	private static final String NOT_CACHED_MESSAGE = "No cached summary yet. Open Profile on RuneBound to begin tracking.";
	private static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	private static final Color CARD_BACKGROUND = new Color(34, 38, 43);
	private static final Color CARD_BORDER = new Color(61, 68, 76);
	private static final Color TEXT_PRIMARY = new Color(235, 239, 244);
	private static final Color TEXT_SECONDARY = new Color(158, 166, 176);
	private static final Color DOT_GREEN = new Color(72, 196, 116);
	private static final Color DOT_GOLD = new Color(230, 176, 73);
	private static final Color DOT_GRAY = new Color(128, 136, 145);
	private static final Color DOT_RED = new Color(218, 87, 76);
	private static final Color CHIP_BACKGROUND = new Color(42, 48, 56);

	private final JLabel playerValue = primaryValueLabel("Not detected");
	private final JLabel currentTitleValue = valueLabel("");
	private final JLabel tierValue = valueLabel("");
	private final JLabel boundPointsValue = primaryStatLabel("");
	private final JLabel totalLevelValue = valueLabel("");
	private final JLabel totalXpValue = valueLabel("");
	private final JTextArea recentAchievementsValue = textAreaValue("");
	private final JLabel freshnessValue = valueLabel("");
	private final JLabel statusDot = new JLabel("●");
	private final JLabel statusValue = valueLabel("Not logged in");
	private final JTextArea statusNoteValue = mutedTextArea("Enter a username or log in.");
	private final JLabel lastRefreshValue = valueLabel("Never");
	private final JLabel cooldownValue = valueLabel("Ready");
	private final JTextArea summaryMessage = mutedTextArea("");
	private final JPanel statusChip = new JPanel(new GridBagLayout());
	private final JTextField manualLookupField = new JTextField();
	private final JButton openProfileButton = new JButton("Open Profile");
	private final JButton refreshButton = new JButton("Refresh");
	private final JLabel summaryHeader = sectionLabel("Overview");
	private final JPanel lastRefreshRow = field("Last lookup", lastRefreshValue);
	private final JPanel cooldownRow = field("Cooldown", cooldownValue);
	private final JPanel currentTitleRow = field("Title", currentTitleValue);
	private final JPanel tierRow = field("Tier", tierValue);
	private final JPanel boundPointsRow = primaryMetric("BoundPoints", boundPointsValue);
	private final JPanel totalLevelRow = field("Total Level", totalLevelValue);
	private final JPanel totalXpRow = field("Total XP", totalXpValue);
	private final JPanel recentAchievementsRow = textAreaField("Achievements", recentAchievementsValue);

	public RuneBoundPanel()
	{
		setLayout(new BorderLayout());
		setBackground(BACKGROUND);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(BACKGROUND);
		content.setBorder(BorderFactory.createEmptyBorder(8, 5, 9, 5));
		stretchHorizontally(content);

		final JLabel title = new JLabel("RuneBound");
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		title.setForeground(TEXT_PRIMARY);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18.0f));
		stretchHorizontally(title);
		content.add(title);

		final JLabel subtitle = new JLabel("Profile companion");
		subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		subtitle.setForeground(TEXT_SECONDARY);
		stretchHorizontally(subtitle);
		content.add(subtitle);
		content.add(Box.createRigidArea(new Dimension(0, 6)));
		content.add(divider());
		content.add(Box.createRigidArea(new Dimension(0, 6)));

		final JTextArea disclosure = mutedTextArea(PRIVACY_NOTE);
		disclosure.setAlignmentX(Component.LEFT_ALIGNMENT);
		disclosure.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		stretchHorizontally(disclosure);
		content.add(disclosure);

		final JPanel lookupPanel = new JPanel();
		lookupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		lookupPanel.setLayout(new BoxLayout(lookupPanel, BoxLayout.Y_AXIS));
		lookupPanel.setBackground(BACKGROUND);
		lookupPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		stretchHorizontally(lookupPanel);

		lookupPanel.add(sectionLabel("Lookup"));
		lookupPanel.add(Box.createRigidArea(new Dimension(0, 4)));

		styleInlineButton(refreshButton);
		refreshButton.setToolTipText("Reads the cached RuneBound summary with a safe GET request. It does not update stats.");

		final JPanel lookupRow = new JPanel(new BorderLayout(6, 0));
		lookupRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		lookupRow.setBackground(BACKGROUND);
		manualLookupField.setToolTipText("Public OSRS username to look up.");
		manualLookupField.setMinimumSize(new Dimension(120, 30));
		manualLookupField.setPreferredSize(new Dimension(180, 30));
		manualLookupField.setFont(manualLookupField.getFont().deriveFont(13.0f));
		lookupRow.add(manualLookupField, BorderLayout.CENTER);
		lookupRow.add(refreshButton, BorderLayout.EAST);
		stretchHorizontally(lookupRow);
		lookupPanel.add(lookupRow);
		final JTextArea lookupHelp = mutedTextArea("Enter a player and refresh.");
		lookupHelp.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		stretchHorizontally(lookupHelp);
		lookupPanel.add(lookupHelp);
		content.add(lookupPanel);

		final JPanel card = new JPanel();
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD_BACKGROUND);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_BORDER),
			BorderFactory.createEmptyBorder(7, 7, 7, 7)
		));
		stretchHorizontally(card);

		card.add(sectionLabel("Player"));
		card.add(playerSummary());
		card.add(dividerWithSpacing());

		styleButton(openProfileButton);
		openProfileButton.setToolTipText("Open this player's RuneBound profile in your browser.");

		summaryHeader.setVisible(false);
		summaryMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
		summaryMessage.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
		summaryMessage.setVisible(false);
		stretchHorizontally(summaryMessage);
		card.add(summaryHeader);
		card.add(summaryMessage);
		card.add(boundPointsRow);
		boundPointsRow.setToolTipText("RuneBound progression score for tracked profile data.");
		boundPointsValue.setToolTipText("RuneBound progression score for tracked profile data.");
		card.add(currentTitleRow);
		card.add(tierRow);
		card.add(totalLevelRow);
		card.add(totalXpRow);
		card.add(recentAchievementsRow);
		hideSummaryRows();
		card.add(dividerWithSpacing());
		card.add(sectionLabel("Activity"));
		card.add(lastRefreshRow);
		card.add(cooldownRow);
		cooldownRow.setToolTipText("Refresh is limited per player to avoid unnecessary RuneBound reads.");
		card.add(dividerWithSpacing());
		card.add(sectionLabel("Actions"));
		card.add(openProfileButton);

		content.add(card);

		openProfileButton.setEnabled(true);
		refreshButton.setEnabled(false);

		add(content, BorderLayout.NORTH);
	}

	void setOpenProfileAction(ActionListener listener)
	{
		openProfileButton.addActionListener(listener);
	}

	void setManualLookupAction(ActionListener listener)
	{
		manualLookupField.addActionListener(listener);
	}

	void setRefreshAction(ActionListener listener)
	{
		refreshButton.addActionListener(listener);
	}

	String manualLookupUsername()
	{
		return manualLookupField.getText();
	}

	void setManualLookupUsername(String username)
	{
		manualLookupField.setText(username);
	}

	void showModel(
		String username,
		String status,
		Instant lastRefresh,
		Duration cooldownRemaining,
		boolean networkLookupsEnabled
	)
	{
		showModel(RuneBoundPanelModel.builder()
			.player(username)
			.status(status)
			.lastLookup(lastRefresh)
			.cooldownRemaining(cooldownRemaining)
			.networkLookupsEnabled(networkLookupsEnabled)
			.openProfileEnabled(RuneBoundUsername.normalize(username) != null)
			.build());
	}

	void showModel(RuneBoundPanelModel model)
	{
		final Runnable render = () -> applyModel(model);

		if (SwingUtilities.isEventDispatchThread())
		{
			render.run();
			return;
		}

		SwingUtilities.invokeLater(render);
	}

	private void applyModel(RuneBoundPanelModel model)
	{
		final RuneBoundPanelModel safeModel = model == null ? RuneBoundPanelModel.builder().status("Not logged in").build() : model;
		final boolean hasUsername = safeModel.hasProfile();
		final PanelStatus panelStatus = resolveStatus(
			hasUsername,
			safeModel.getStatus(),
			safeModel.getCooldownRemaining(),
			safeModel.getFreshness()
		);

		playerValue.setText(hasUsername ? safeModel.getPlayer() : "Not detected");
		final String freshness = RuneBoundUsername.normalize(safeModel.getFreshness());
		freshnessValue.setText(freshness == null ? "" : freshness);
		final boolean hasCurrentTitle = setOptionalText(currentTitleRow, currentTitleValue, safeModel.getCurrentTitle());
		final boolean hasTier = setOptionalText(tierRow, tierValue, safeModel.getTier());
		final boolean hasBoundPoints = setOptionalText(boundPointsRow, boundPointsValue, safeModel.getBoundPoints());
		final boolean hasTotalLevel = setOptionalText(totalLevelRow, totalLevelValue, safeModel.getTotalLevel());
		final boolean hasTotalXp = setOptionalText(totalXpRow, totalXpValue, safeModel.getTotalXp());
		final boolean hasRecentAchievements = setOptionalText(recentAchievementsRow, recentAchievementsValue, safeModel.getRecentAchievements());
		final boolean hasSummaryDetails = hasCurrentTitle || hasTier || hasBoundPoints || hasTotalLevel || hasTotalXp || hasRecentAchievements;
		final boolean isNotCached = "No cached RuneBound summary".equals(safeModel.getStatus());
		summaryMessage.setText(isNotCached ? NOT_CACHED_MESSAGE : "");
		summaryMessage.setVisible(isNotCached);
		summaryHeader.setVisible(hasSummaryDetails || isNotCached);
		statusDot.setForeground(panelStatus.dotColor);
		statusValue.setText(panelStatus.text);
		statusNoteValue.setText(panelStatus.note);
		statusChip.setToolTipText(panelStatus.note);
		statusDot.setToolTipText(panelStatus.note);
		statusValue.setToolTipText(panelStatus.note);
		statusChip.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(panelStatus.dotColor),
			BorderFactory.createEmptyBorder(2, 6, 2, 6)
		));
		lastRefreshValue.setText(formatLastLookup(safeModel.getLastLookup(), Instant.now()));
		cooldownValue.setText(formatDuration(safeModel.getCooldownRemaining()));
		openProfileButton.setEnabled(true);
		refreshButton.setEnabled(safeModel.isNetworkLookupsEnabled());
	}

	static String formatDuration(Duration duration)
	{
		if (duration == null || duration.isZero() || duration.isNegative())
		{
			return "Ready";
		}

		final long seconds = duration.getSeconds();
		final long minutes = seconds / 60;
		final long remainingSeconds = seconds % 60;
		if (minutes > 0)
		{
			return minutes + "m " + remainingSeconds + "s";
		}

		return remainingSeconds + "s";
	}

	static String formatLastLookup(Instant lastLookup, Instant now)
	{
		if (lastLookup == null)
		{
			return "Never";
		}

		final Duration age = Duration.between(lastLookup, now == null ? Instant.now() : now);
		if (age.isNegative() || age.getSeconds() < 30)
		{
			return "Just now";
		}

		final long minutes = age.toMinutes();
		if (minutes < 60)
		{
			return minutes + "m ago";
		}

		final long hours = age.toHours();
		if (hours < 24)
		{
			return hours + "h ago";
		}

		final long days = age.toDays();
		return days + "d ago";
	}

	String displayedPlayer()
	{
		return playerValue.getText();
	}

	String displayedCurrentTitle()
	{
		return currentTitleValue.getText();
	}

	String displayedTier()
	{
		return tierValue.getText();
	}

	String displayedBoundPoints()
	{
		return boundPointsValue.getText();
	}

	String displayedTotalLevel()
	{
		return totalLevelValue.getText();
	}

	String displayedTotalXp()
	{
		return totalXpValue.getText();
	}

	String displayedRecentAchievements()
	{
		return recentAchievementsValue.getText();
	}

	String displayedFreshness()
	{
		return freshnessValue.getText();
	}

	String displayedSummaryMessage()
	{
		return summaryMessage.getText();
	}

	String displayedStatus()
	{
		return statusValue.getText();
	}

	String displayedStatusNote()
	{
		return statusNoteValue.getText();
	}

	String displayedCooldown()
	{
		return cooldownValue.getText();
	}

	String displayedLastLookup()
	{
		return lastRefreshValue.getText();
	}

	boolean isOpenProfileEnabled()
	{
		return openProfileButton.isEnabled();
	}

	boolean isRefreshEnabled()
	{
		return refreshButton.isEnabled();
	}

	boolean isCurrentTitleVisible()
	{
		return currentTitleRow.isVisible();
	}

	boolean isTierVisible()
	{
		return tierRow.isVisible();
	}

	boolean isBoundPointsVisible()
	{
		return boundPointsRow.isVisible();
	}

	boolean isTotalLevelVisible()
	{
		return totalLevelRow.isVisible();
	}

	boolean isTotalXpVisible()
	{
		return totalXpRow.isVisible();
	}

	boolean isRecentAchievementsVisible()
	{
		return recentAchievementsRow.isVisible();
	}

	boolean isFreshnessVisible()
	{
		return false;
	}

	boolean isSummaryMessageVisible()
	{
		return summaryMessage.isVisible();
	}

	boolean isSummaryHeaderVisible()
	{
		return summaryHeader.isVisible();
	}

	String refreshButtonText()
	{
		return refreshButton.getText();
	}

	String openProfileButtonText()
	{
		return openProfileButton.getText();
	}

	Color statusDotColor()
	{
		return statusDot.getForeground();
	}

	String refreshTooltip()
	{
		return refreshButton.getToolTipText();
	}

	String statusTooltip()
	{
		return statusChip.getToolTipText();
	}

	String boundPointsTooltip()
	{
		return boundPointsRow.getToolTipText();
	}

	String cooldownTooltip()
	{
		return cooldownRow.getToolTipText();
	}

	int manualLookupPreferredWidth()
	{
		return manualLookupField.getPreferredSize().width;
	}

	int refreshPreferredWidth()
	{
		return refreshButton.getPreferredSize().width;
	}


	private static JPanel field(String labelText, JLabel value)
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setBackground(CARD_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		stretchHorizontally(panel);

		final JLabel label = new JLabel(labelText);
		label.setForeground(TEXT_SECONDARY);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 11.0f));
		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = 0;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(0, 0, 0, 8);
		panel.add(label, labelConstraints);

		value.setHorizontalAlignment(SwingConstants.RIGHT);
		final GridBagConstraints valueConstraints = new GridBagConstraints();
		valueConstraints.gridx = 1;
		valueConstraints.gridy = 0;
		valueConstraints.weightx = 1.0;
		valueConstraints.fill = GridBagConstraints.HORIZONTAL;
		valueConstraints.anchor = GridBagConstraints.EAST;
		panel.add(value, valueConstraints);
		return panel;
	}

	private static JPanel primaryMetric(String labelText, JLabel value)
	{
		final JPanel panel = new JPanel();
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(CARD_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		stretchHorizontally(panel);

		final JLabel label = new JLabel(labelText);
		label.setForeground(TEXT_SECONDARY);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 11.0f));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(label);
		panel.add(value);
		return panel;
	}

	private void hideSummaryRows()
	{
		currentTitleRow.setVisible(false);
		tierRow.setVisible(false);
		boundPointsRow.setVisible(false);
		totalLevelRow.setVisible(false);
		totalXpRow.setVisible(false);
		recentAchievementsRow.setVisible(false);
	}

	private static JPanel textAreaField(String labelText, JTextArea value)
	{
		final JPanel panel = new JPanel();
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(CARD_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		stretchHorizontally(panel);

		final JLabel label = new JLabel(labelText);
		label.setForeground(TEXT_SECONDARY);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 11.0f));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setRows(3);
		stretchHorizontally(value);
		panel.add(label);
		panel.add(Box.createRigidArea(new Dimension(0, 2)));
		panel.add(value);
		return panel;
	}

	private JPanel playerSummary()
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setBackground(CARD_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		stretchHorizontally(panel);

		final GridBagConstraints playerConstraints = new GridBagConstraints();
		playerConstraints.gridx = 0;
		playerConstraints.gridy = 0;
		playerConstraints.weightx = 1.0;
		playerConstraints.fill = GridBagConstraints.HORIZONTAL;
		playerConstraints.anchor = GridBagConstraints.WEST;
		playerConstraints.insets = new Insets(0, 0, 0, 8);
		playerValue.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(playerValue, playerConstraints);

		final GridBagConstraints chipConstraints = new GridBagConstraints();
		chipConstraints.gridx = 1;
		chipConstraints.gridy = 0;
		chipConstraints.anchor = GridBagConstraints.EAST;
		panel.add(statusChip(), chipConstraints);
		return panel;
	}

	private JPanel statusChip()
	{
		statusChip.setAlignmentX(Component.LEFT_ALIGNMENT);
		statusChip.setBackground(CHIP_BACKGROUND);
		statusChip.setOpaque(true);
		statusChip.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(DOT_GRAY),
			BorderFactory.createEmptyBorder(2, 6, 2, 6)
		));

		final GridBagConstraints dotConstraints = new GridBagConstraints();
		dotConstraints.gridx = 0;
		dotConstraints.gridy = 0;
		dotConstraints.anchor = GridBagConstraints.WEST;
		dotConstraints.insets = new Insets(0, 0, 0, 5);
		statusDot.setForeground(DOT_GRAY);
		statusChip.add(statusDot, dotConstraints);

		final GridBagConstraints valueConstraints = new GridBagConstraints();
		valueConstraints.gridx = 1;
		valueConstraints.gridy = 0;
		valueConstraints.anchor = GridBagConstraints.WEST;
		statusValue.setFont(statusValue.getFont().deriveFont(Font.BOLD, 11.0f));
		statusChip.add(statusValue, valueConstraints);
		return statusChip;
	}

	private static void styleButton(JButton button)
	{
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMinimumSize(new Dimension(0, 28));
		button.setPreferredSize(new Dimension(180, 28));
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		button.setFocusPainted(false);
		button.setHorizontalAlignment(SwingConstants.CENTER);
	}

	private static void styleInlineButton(JButton button)
	{
		// Wide enough that the "Refresh" label is never clipped across look-and-feels/fonts.
		final Dimension size = new Dimension(92, 30);
		button.setMinimumSize(size);
		button.setPreferredSize(size);
		button.setMaximumSize(size);
		button.setMargin(new Insets(0, 6, 0, 6));
		button.setFocusPainted(false);
		button.setHorizontalAlignment(SwingConstants.CENTER);
	}

	private static void stretchHorizontally(Component component)
	{
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));
	}

	private static JPanel divider()
	{
		final JPanel divider = new JPanel();
		divider.setAlignmentX(Component.LEFT_ALIGNMENT);
		divider.setBackground(CARD_BORDER);
		divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		return divider;
	}

	private static JPanel dividerWithSpacing()
	{
		final JPanel panel = new JPanel();
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(CARD_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(1, 0, 7, 0));
		panel.add(divider());
		return panel;
	}

	private static PanelStatus resolveStatus(
		boolean hasUsername,
		String status,
		Duration cooldownRemaining,
		String freshness
	)
	{
		if (!hasUsername)
		{
			return new PanelStatus("Unavailable", "Enter a username or log in.", DOT_GRAY);
		}

		final boolean cooldownActive = cooldownRemaining != null && !cooldownRemaining.isZero() && !cooldownRemaining.isNegative();
		if (cooldownActive && status != null && status.startsWith("Cooldown active"))
		{
			return freshnessStatus(freshness, "Try again when cooldown ends.");
		}

		if ("Network lookups disabled".equals(status))
		{
			return new PanelStatus("Unavailable", "Enable lookups to use Refresh.", DOT_GRAY);
		}

		if ("Loading RuneBound summary".equals(status))
		{
			return new PanelStatus("Unavailable", "Fetching RuneBound summary.", DOT_GOLD);
		}

		if ("Cached RuneBound profile".equals(status))
		{
			return freshnessStatus(freshness, "Cached summary ready.");
		}

		if ("Summary response loaded from RuneBound".equals(status))
		{
			return freshnessStatus(freshness, "Cached summary ready.");
		}

		if ("Using local summary cache".equals(status))
		{
			return freshnessStatus(freshness, "Using recent lookup.");
		}

		if ("Showing cached summary while refreshing".equals(status))
		{
			return freshnessStatus(freshness, "Refreshing summary.");
		}

		if ("Summary lookup could not reach RuneBound".equals(status))
		{
			return new PanelStatus("Error", "RuneBound unreachable.", DOT_RED);
		}

		if ("No cached RuneBound summary".equals(status))
		{
			return new PanelStatus("Not cached", "Open Profile to begin tracking.", DOT_GRAY);
		}

		if ("Invalid username".equals(status))
		{
			return new PanelStatus("Error", "Enter a valid OSRS username.", DOT_RED);
		}

		if ("Profile link unavailable".equals(status))
		{
			return new PanelStatus("Unavailable", "Open Profile is unavailable.", DOT_GRAY);
		}

		if ("Cached RuneBound profile may be stale".equals(status))
		{
			return new PanelStatus("Stale", "RuneBound says this cached summary may be old.", DOT_GOLD);
		}

		if ("Cached RuneBound profile needs freshness review".equals(status))
		{
			return new PanelStatus("Stale", "RuneBound marked this cached summary for review.", DOT_GOLD);
		}

		if ("RuneBound summary unavailable".equals(status))
		{
			return new PanelStatus("Unavailable", "Summary is not displayable.", DOT_GRAY);
		}

		if ("RuneBound summary service unavailable".equals(status))
		{
			return new PanelStatus("Error", "Try again later.", DOT_RED);
		}

		if ("RuneBound summary endpoint unavailable".equals(status))
		{
			return new PanelStatus("Error", "Summary endpoint unavailable.", DOT_RED);
		}

		if ("RuneBound summary response was too large".equals(status))
		{
			return new PanelStatus("Error", "Summary was not loaded.", DOT_RED);
		}

		if ("RuneBound rate limit active".equals(status))
		{
			return new PanelStatus("Error", "Try again later.", DOT_RED);
		}

		if ("No logged-in player detected".equals(status))
		{
			return new PanelStatus("Unavailable", "Enter a username or log in.", DOT_GRAY);
		}

		if (status != null && status.startsWith("RuneBound "))
		{
			return new PanelStatus("Error", "Lookup failed safely.", DOT_RED);
		}

		return new PanelStatus("Unavailable", "Ready to refresh.", DOT_GRAY);
	}

	private static PanelStatus freshnessStatus(String freshness, String note)
	{
		if ("Fresh".equals(freshness))
		{
			return new PanelStatus("Fresh", note, DOT_GREEN);
		}
		if ("Stale".equals(freshness))
		{
			return new PanelStatus("Stale", "RuneBound says this cached summary may be old.", DOT_GOLD);
		}
		if ("Not cached".equals(freshness))
		{
			return new PanelStatus("Not cached", "Open Profile to begin tracking.", DOT_GRAY);
		}
		if ("Unavailable".equals(freshness))
		{
			return new PanelStatus("Unavailable", "Summary is not displayable.", DOT_GRAY);
		}
		return new PanelStatus("Unavailable", note, DOT_GRAY);
	}

	private static JLabel valueLabel(String text)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(TEXT_PRIMARY);
		return label;
	}

	private static JTextArea textAreaValue(String text)
	{
		final JTextArea area = new JTextArea(text);
		area.setEditable(false);
		area.setFocusable(false);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setOpaque(false);
		area.setForeground(TEXT_PRIMARY);
		// Keep a consistent LEFT alignment in BoxLayout columns. A stray CENTER-aligned
		// child makes BoxLayout reserve center space and collapse sibling rows (e.g. the
		// lookup field) below their available width.
		area.setAlignmentX(Component.LEFT_ALIGNMENT);
		return area;
	}

	private static JTextArea mutedTextArea(String text)
	{
		final JTextArea area = textAreaValue(text);
		area.setForeground(TEXT_SECONDARY);
		return area;
	}

	private static JLabel primaryValueLabel(String text)
	{
		final JLabel label = valueLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 16.0f));
		return label;
	}

	private static JLabel primaryStatLabel(String text)
	{
		final JLabel label = valueLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 20.0f));
		return label;
	}

	private static boolean setOptionalText(JPanel row, JLabel valueLabel, String value)
	{
		final String normalized = RuneBoundUsername.normalize(value);
		valueLabel.setText(normalized == null ? "" : normalized);
		row.setVisible(normalized != null);
		return normalized != null;
	}

	private static boolean setOptionalText(JPanel row, JTextArea valueArea, String value)
	{
		final String normalized = RuneBoundUsername.normalize(value);
		valueArea.setText(normalized == null ? "" : normalized);
		row.setVisible(normalized != null);
		return normalized != null;
	}

	private static JLabel sectionLabel(String text)
	{
		final JLabel label = new JLabel(text);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setForeground(TEXT_SECONDARY);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 11.0f));
		label.setBorder(BorderFactory.createEmptyBorder(1, 0, 5, 0));
		return label;
	}

	private static final class PanelStatus
	{
		private final String text;
		private final String note;
		private final Color dotColor;

		private PanelStatus(String text, String note, Color dotColor)
		{
			this.text = text;
			this.note = note;
			this.dotColor = dotColor;
		}
	}
}
