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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class RuneBoundPanel extends PluginPanel
{
	private static final long serialVersionUID = 1L;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss z")
		.withZone(ZoneId.systemDefault());
	private static final String PRIVACY_NOTE = "Explicit rune-bound.net lookups only. No gameplay data is sent or automated.";
	private static final String NOT_CACHED_MESSAGE = "RuneBound has no cached summary for this player yet. Open RuneBound to begin tracking or update this profile.";
	private static final String ACTION_NOTE = "Refresh reads RuneBound's cache. Update opens RuneBound in your browser.";
	private static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
	private static final Color CARD_BACKGROUND = new Color(34, 38, 43);
	private static final Color CARD_BORDER = new Color(61, 68, 76);
	private static final Color TEXT_PRIMARY = new Color(235, 239, 244);
	private static final Color TEXT_SECONDARY = new Color(158, 166, 176);
	private static final Color DOT_GREEN = new Color(72, 196, 116);
	private static final Color DOT_GOLD = new Color(230, 176, 73);
	private static final Color DOT_GRAY = new Color(128, 136, 145);

	private final JLabel playerValue = primaryValueLabel("Not detected");
	private final JLabel currentTitleValue = valueLabel("");
	private final JLabel tierValue = valueLabel("");
	private final JLabel boundPointsValue = valueLabel("");
	private final JLabel totalLevelValue = valueLabel("");
	private final JLabel totalXpValue = valueLabel("");
	private final JTextArea recentAchievementsValue = textAreaValue("");
	private final JLabel freshnessValue = valueLabel("");
	private final JLabel statusDot = new JLabel("●");
	private final JLabel statusValue = valueLabel("Not logged in");
	private final JTextArea statusNoteValue = mutedTextArea("Log in or use manual lookup.");
	private final JLabel lastRefreshValue = valueLabel("Never");
	private final JLabel cooldownValue = valueLabel("Ready");
	private final JTextArea summaryMessage = mutedTextArea("");
	private final JTextArea actionNote = mutedTextArea(ACTION_NOTE);
	private final JTextField manualLookupField = new JTextField();
	private final JButton lookupButton = new JButton("Lookup");
	private final JButton openSearchButton = new JButton("Open RuneBound");
	private final JButton openProfileButton = new JButton("Open Profile");
	private final JButton updateOnRuneBoundButton = new JButton("Update on RuneBound");
	private final JButton refreshButton = new JButton("Refresh");
	private final JLabel summaryHeader = sectionLabel("Overview");
	private final JPanel freshnessRow = field("Freshness", freshnessValue);
	private final JPanel lastRefreshRow = field("Last lookup", lastRefreshValue);
	private final JPanel cooldownRow = field("Cooldown", cooldownValue);
	private final JPanel currentTitleRow = field("Title", currentTitleValue);
	private final JPanel tierRow = field("Tier", tierValue);
	private final JPanel boundPointsRow = field("BoundPoints", boundPointsValue);
	private final JPanel totalLevelRow = field("Total Level", totalLevelValue);
	private final JPanel totalXpRow = field("Total XP", totalXpValue);
	private final JPanel recentAchievementsRow = textAreaField("Recent Achievements", recentAchievementsValue);

	public RuneBoundPanel()
	{
		setLayout(new BorderLayout());
		setBackground(BACKGROUND);

		final JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(BACKGROUND);
		content.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

		final JLabel title = new JLabel("RuneBound");
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		title.setForeground(TEXT_PRIMARY);
		title.setFont(title.getFont().deriveFont(Font.BOLD, 18.0f));
		content.add(title);

		final JLabel subtitle = new JLabel("Cached profile summaries");
		subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		subtitle.setForeground(TEXT_SECONDARY);
		content.add(subtitle);
		content.add(Box.createRigidArea(new Dimension(0, 8)));
		content.add(divider());
		content.add(Box.createRigidArea(new Dimension(0, 8)));

		final JTextArea disclosure = mutedTextArea(PRIVACY_NOTE);
		disclosure.setAlignmentX(Component.LEFT_ALIGNMENT);
		disclosure.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		content.add(disclosure);

		final JPanel lookupPanel = new JPanel();
		lookupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		lookupPanel.setLayout(new BoxLayout(lookupPanel, BoxLayout.Y_AXIS));
		lookupPanel.setBackground(BACKGROUND);
		lookupPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		lookupPanel.add(sectionLabel("Lookup"));
		lookupPanel.add(Box.createRigidArea(new Dimension(0, 4)));

		final JPanel lookupRow = new JPanel(new BorderLayout(6, 0));
		lookupRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		lookupRow.setBackground(BACKGROUND);
		manualLookupField.setToolTipText("Public OSRS username");
		lookupRow.add(manualLookupField, BorderLayout.CENTER);
		styleButton(lookupButton);
		lookupButton.setToolTipText("Request the read-only RuneBound summary for the typed username.");
		lookupButton.setMaximumSize(new Dimension(82, 30));
		lookupRow.add(lookupButton, BorderLayout.EAST);
		lookupPanel.add(lookupRow);
		content.add(lookupPanel);

		final JPanel card = new JPanel();
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD_BACKGROUND);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(CARD_BORDER),
			BorderFactory.createEmptyBorder(9, 9, 9, 9)
		));
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

		card.add(sectionLabel("Player"));
		card.add(field("Player", playerValue));
		card.add(statusField());
		card.add(dividerWithSpacing());
		summaryHeader.setVisible(false);
		summaryMessage.setAlignmentX(Component.LEFT_ALIGNMENT);
		summaryMessage.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 0));
		summaryMessage.setVisible(false);
		card.add(summaryHeader);
		card.add(summaryMessage);
		card.add(boundPointsRow);
		card.add(currentTitleRow);
		card.add(tierRow);
		card.add(totalLevelRow);
		card.add(totalXpRow);
		card.add(recentAchievementsRow);
		hideSummaryRows();
		card.add(dividerWithSpacing());
		card.add(sectionLabel("Freshness"));
		freshnessRow.setVisible(false);
		card.add(freshnessRow);
		card.add(lastRefreshRow);
		card.add(cooldownRow);
		card.add(dividerWithSpacing());

		styleButton(refreshButton);
		styleButton(openProfileButton);
		styleButton(updateOnRuneBoundButton);
		styleButton(openSearchButton);
		refreshButton.setToolTipText("Request the read-only RuneBound summary for the active player.");
		openProfileButton.setToolTipText("Open this player's RuneBound profile in your browser.");
		updateOnRuneBoundButton.setToolTipText("Open RuneBound's search/update page in your browser. No plugin update request is sent.");
		openSearchButton.setToolTipText("Open RuneBound in your browser.");
		card.add(sectionLabel("Actions"));
		actionNote.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
		card.add(actionNote);
		card.add(refreshButton);
		card.add(Box.createRigidArea(new Dimension(0, 6)));
		card.add(openProfileButton);
		card.add(Box.createRigidArea(new Dimension(0, 6)));
		card.add(updateOnRuneBoundButton);
		card.add(Box.createRigidArea(new Dimension(0, 6)));
		card.add(openSearchButton);

		content.add(card);

		lookupButton.setEnabled(false);
		openSearchButton.setEnabled(true);
		openProfileButton.setEnabled(false);
		updateOnRuneBoundButton.setEnabled(true);
		refreshButton.setEnabled(false);

		add(content, BorderLayout.NORTH);
	}

	void setOpenSearchAction(ActionListener listener)
	{
		openSearchButton.addActionListener(listener);
	}

	void setOpenProfileAction(ActionListener listener)
	{
		openProfileButton.addActionListener(listener);
	}

	void setUpdateOnRuneBoundAction(ActionListener listener)
	{
		updateOnRuneBoundButton.addActionListener(listener);
	}

	void setManualLookupAction(ActionListener listener)
	{
		lookupButton.addActionListener(listener);
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
		final PanelStatus panelStatus = resolveStatus(hasUsername, safeModel.getStatus(), safeModel.getCooldownRemaining());

		playerValue.setText(hasUsername ? safeModel.getPlayer() : "Not detected");
		final boolean hasFreshness = setOptionalText(freshnessRow, freshnessValue, safeModel.getFreshness());
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
		freshnessRow.setVisible(hasFreshness);
		statusDot.setForeground(panelStatus.dotColor);
		statusValue.setText(panelStatus.text);
		statusNoteValue.setText(panelStatus.note);
		lastRefreshValue.setText(safeModel.getLastLookup() == null ? "Never" : TIME_FORMATTER.format(safeModel.getLastLookup()));
		cooldownValue.setText(formatDuration(safeModel.getCooldownRemaining()));
		lookupButton.setEnabled(safeModel.isNetworkLookupsEnabled());
		openSearchButton.setEnabled(true);
		openProfileButton.setEnabled(safeModel.canOpenProfile());
		updateOnRuneBoundButton.setEnabled(true);
		refreshButton.setEnabled(hasUsername && safeModel.isNetworkLookupsEnabled());
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

	boolean isOpenProfileEnabled()
	{
		return openProfileButton.isEnabled();
	}

	boolean isOpenSearchEnabled()
	{
		return openSearchButton.isEnabled();
	}

	boolean isUpdateOnRuneBoundEnabled()
	{
		return updateOnRuneBoundButton.isEnabled();
	}

	boolean isRefreshEnabled()
	{
		return refreshButton.isEnabled();
	}

	boolean isLookupEnabled()
	{
		return lookupButton.isEnabled();
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
		return freshnessRow.isVisible();
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

	String openSearchButtonText()
	{
		return openSearchButton.getText();
	}

	String updateOnRuneBoundButtonText()
	{
		return updateOnRuneBoundButton.getText();
	}

	Color statusDotColor()
	{
		return statusDot.getForeground();
	}

	private static JPanel field(String labelText, JLabel value)
	{
		final JPanel panel = new JPanel();
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(CARD_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 0));

		final JLabel label = new JLabel(labelText);
		label.setForeground(TEXT_SECONDARY);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setAlignmentX(Component.LEFT_ALIGNMENT);
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
		final JPanel panel = field(labelText, new JLabel());
		panel.removeAll();

		final JLabel label = new JLabel(labelText);
		label.setForeground(TEXT_SECONDARY);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		value.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(label);
		panel.add(value);
		return panel;
	}

	private JPanel statusField()
	{
		final JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setBackground(CARD_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 0));

		final GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = 0;
		labelConstraints.gridwidth = 2;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(0, 0, 2, 0);

		final JLabel label = new JLabel("Status");
		label.setForeground(TEXT_SECONDARY);
		panel.add(label, labelConstraints);

		final GridBagConstraints dotConstraints = new GridBagConstraints();
		dotConstraints.gridx = 0;
		dotConstraints.gridy = 1;
		dotConstraints.anchor = GridBagConstraints.WEST;
		dotConstraints.insets = new Insets(0, 0, 0, 6);
		statusDot.setForeground(DOT_GRAY);
		panel.add(statusDot, dotConstraints);

		final GridBagConstraints valueConstraints = new GridBagConstraints();
		valueConstraints.gridx = 1;
		valueConstraints.gridy = 1;
		valueConstraints.weightx = 1.0;
		valueConstraints.fill = GridBagConstraints.HORIZONTAL;
		valueConstraints.anchor = GridBagConstraints.WEST;
		panel.add(statusValue, valueConstraints);

		final GridBagConstraints noteConstraints = new GridBagConstraints();
		noteConstraints.gridx = 0;
		noteConstraints.gridy = 2;
		noteConstraints.gridwidth = 2;
		noteConstraints.weightx = 1.0;
		noteConstraints.fill = GridBagConstraints.HORIZONTAL;
		noteConstraints.anchor = GridBagConstraints.WEST;
		noteConstraints.insets = new Insets(3, 0, 0, 0);
		panel.add(statusNoteValue, noteConstraints);
		return panel;
	}

	private static void styleButton(JButton button)
	{
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		button.setFocusPainted(false);
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
		panel.setBorder(BorderFactory.createEmptyBorder(2, 0, 9, 0));
		panel.add(divider());
		return panel;
	}

	private static PanelStatus resolveStatus(
		boolean hasUsername,
		String status,
		Duration cooldownRemaining
	)
	{
		if (!hasUsername)
		{
			return new PanelStatus("Not logged in", "Log in or use manual lookup.", DOT_GRAY);
		}

		final boolean cooldownActive = cooldownRemaining != null && !cooldownRemaining.isZero() && !cooldownRemaining.isNegative();
		if (cooldownActive && status != null && status.startsWith("Cooldown active"))
		{
			return new PanelStatus("Cooldown active", "Wait before another summary lookup for this player.", DOT_GOLD);
		}

		if ("Network lookups disabled".equals(status))
		{
			return new PanelStatus("Lookups disabled", "Enable summary lookups in config to request cached data.", DOT_GOLD);
		}

		if ("Loading RuneBound summary".equals(status))
		{
			return new PanelStatus("Loading summary", "Requesting cached RuneBound summary.", DOT_GOLD);
		}

		if ("Summary response loaded from RuneBound".equals(status))
		{
			return new PanelStatus("Cached summary loaded", "Read-only summary returned by RuneBound cache.", DOT_GREEN);
		}

		if ("Using local summary cache".equals(status))
		{
			return new PanelStatus("Cached locally", "Shown from local plugin cache.", DOT_GREEN);
		}

		if ("Showing cached summary while refreshing".equals(status))
		{
			return new PanelStatus("Refreshing summary", "Showing cache until the latest request returns.", DOT_GOLD);
		}

		if ("Summary lookup could not reach RuneBound".equals(status))
		{
			return new PanelStatus("Network unavailable", "Check connection; no background retry is running.", DOT_GOLD);
		}

		if ("No cached RuneBound summary".equals(status))
		{
			return new PanelStatus("Not cached", "No cached profile summary yet.", DOT_GOLD);
		}

		if ("Invalid username".equals(status))
		{
			return new PanelStatus("Invalid username", "Enter a valid public OSRS username.", DOT_GOLD);
		}

		if ("Profile link unavailable".equals(status))
		{
			return new PanelStatus("Profile unavailable", "RuneBound did not provide a safe profile link.", DOT_GOLD);
		}

		if ("Cached RuneBound profile may be stale".equals(status))
		{
			return new PanelStatus("Stale cache", "Freshness labels show age and trust.", DOT_GOLD);
		}

		if ("Cached RuneBound profile needs freshness review".equals(status))
		{
			return new PanelStatus("Dirty cache", "RuneBound marked this cached summary for review.", DOT_GOLD);
		}

		if ("RuneBound summary unavailable".equals(status))
		{
			return new PanelStatus("Summary unavailable", "RuneBound returned a malformed cached summary.", DOT_GOLD);
		}

		if ("RuneBound summary service unavailable".equals(status))
		{
			return new PanelStatus("Server error", "RuneBound could not serve the summary right now.", DOT_GOLD);
		}

		if ("RuneBound summary response was too large".equals(status))
		{
			return new PanelStatus("Summary unavailable", "RuneBound returned more data than this plugin accepts.", DOT_GOLD);
		}

		if ("RuneBound rate limit active".equals(status))
		{
			return new PanelStatus("Rate limited", "Wait before requesting another summary.", DOT_GOLD);
		}

		if ("No logged-in player detected".equals(status))
		{
			return new PanelStatus("Not logged in", "Log in or use manual lookup.", DOT_GRAY);
		}

		if (status != null && status.startsWith("RuneBound "))
		{
			return new PanelStatus("RuneBound unavailable", "The lookup failed safely without retrying in the background.", DOT_GOLD);
		}

		return new PanelStatus("Ready", "Ready for an explicit summary lookup.", DOT_GREEN);
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
		label.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
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
