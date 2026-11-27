' Póster vertical para películas y series.
sub init()
    W = 234
    H = 330
    m.ring = uiRing(m.top, 6, 6, W, H, 18)
    m.bg = uiBox(m.top, 6, 6, W, H, "0x12235FFF", 18)
    m.fallback = uiLabel(m.top, "", 24, 140, 30, "0xF5F7FFFF", "display", W - 36, 4, "center")
    m.img = uiImage(m.top, "", 6, 6, W, H, "scaleToZoom")
    m.bar = m.top.createChild("Group")
    m.name = uiLabel(m.top, "", 6, H + 18, 22, "0xC8D2F5FF", "semibold", W, 2)
    m.W = W
    m.H = H
end sub

sub onContent()
    n = m.top.itemContent
    if n = invalid then return
    m.name.text = n.title
    m.fallback.text = n.title
    if n.HDPosterUrl <> invalid then m.img.uri = n.HDPosterUrl else m.img.uri = ""
    uiClear(m.bar)
    pr = 0
    if n.hasField("prog") then pr = n.prog
    if pr > 0 then
        uiBox(m.bar, 6, 6 + m.H - 8, m.W, 8, "0x0A1747B3", 0)
        uiBox(m.bar, 6, 6 + m.H - 8, m.W * pr, 8, "0x3DE0BEFF", 0)
    end if
end sub

sub onFocus()
    f = m.top.gridHasFocus and m.top.focusPercent > 0.5
    m.ring.visible = f
    if f then m.name.color = "0xFFFFFFFF" else m.name.color = "0xC8D2F5FF"
end sub
