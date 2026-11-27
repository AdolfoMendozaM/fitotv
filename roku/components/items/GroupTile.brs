' Mosaico de grupo: ícono, nombre y cuántos canales trae.
sub init()
    W = 390
    H = 144
    m.ring = uiRing(m.top, 6, 6, W, H, 24)
    m.bg = uiBox(m.top, 6, 6, W, H, "0x12235FFF", 24)
    m.iconBg = uiBox(m.top, 33, 6 + (H - 72) / 2, 72, 72, "0x1A2E78FF", 21)
    m.icon = uiIcon(m.top, "grid", 51, 6 + (H - 36) / 2, 36, "0xFFFFFFFF")
    m.name = uiLabel(m.top, "", 129, 6 + 34, 27, "0xF5F7FFFF", "semibold", W - 140)
    m.count = uiLabel(m.top, "", 129, 6 + 78, 21, "0xAEBBEBFF", "regular", W - 140)
    m.selected = false
end sub

sub onContent()
    n = m.top.itemContent
    if n = invalid then return
    m.name.text = n.title
    m.count.text = n.description
    m.icon.uri = "pkg:/images/icons/" + n.shortDescriptionLine1 + ".png"
    m.selected = n.shortDescriptionLine2 = "1"
    onFocus()
end sub

sub onFocus()
    f = m.top.gridHasFocus and m.top.focusPercent > 0.5
    m.ring.visible = f
    col = "0x12235FFF"
    if m.selected or f then col = "0x1A2E78FF"
    m.bg.blendColor = col
    if m.selected then m.iconBg.blendColor = "0x2350E0FF" else m.iconBg.blendColor = "0x1A2E78FF"
end sub
