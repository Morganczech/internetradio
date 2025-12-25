package cz.internetradio.app.widget

import cz.internetradio.app.R

class CompactWidgetProvider : RadioWidgetProvider() {
    override fun getLayoutId() = R.layout.widget_player_compact
}

class ControlWidgetProvider : RadioWidgetProvider() {
    override fun getLayoutId() = R.layout.widget_player_control
}

class LargeWidgetProvider : RadioWidgetProvider() {
    override fun getLayoutId() = R.layout.widget_player_large
}

class MediumWidgetProvider : RadioWidgetProvider() {
    override fun getLayoutId() = R.layout.widget_player_medium
}
