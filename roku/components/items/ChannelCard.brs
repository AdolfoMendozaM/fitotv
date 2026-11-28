' Tarjeta de canal: número arriba, logo grande en medio, nombre abajo.
sub init()
    W = 318
    H = 186
    m.ring = uiRing(m.top, 6, 6, W, H, 18)
    m.bg = uiBox(m.top, 6, 6, W, H, "0x12235FFF", 18)
    m.num = uiLabel(m.top, "", 24, 16, 27, "0x9DB2FFFF", "display", 120)
    m.logo = uiImage(m.top, "", 48, 48, W - 84, 84)
    m.tile = m.top.createChild("Group")
    m.name = uiLabel(m.top, "", 18, 146, 24, "0xF5F7FFFF", "semibold", W - 18, 1, "center")
end sub

sub onContent()
    n = m.top.itemContent
    if n = invalid then return
    if n.num <> invalid and n.num > 0 then m.num.text = n.num.ToStr() else m.num.text = ""
    m.name.text = n.title
    uiClear(m.tile)
    if n.HDPosterUrl <> invalid and n.HDPosterUrl <> "" then
        m.logo.uri = n.HDPosterUrl
        m.logo.visible = true
    else
        m.logo.visible = false
        m.logo.uri = ""
        uiBox(m.tile, 117, 46, 96, 88, tileFor(n.title), 18)
        uiLabel(m.tile, initials(n.title), 117, 66, 36, "0xFFFFFFFF", "bold", 96, 1, "center")
    end if
end sub

sub onFocus()
    f = m.top.gridHasFocus and m.top.focusPercent > 0.5
    m.ring.visible = f
    if f then m.bg.blendColor = "0x1A2E78FF" else m.bg.blendColor = "0x12235FFF"
end sub
