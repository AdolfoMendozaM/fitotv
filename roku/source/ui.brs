' Ayudantes para dibujar la interfaz con nodos de SceneGraph (pantalla de 1920x1080).

function palette() as object
    if m.colors = invalid then m.colors = fcColors()
    return m.colors
end function

' kind: regular, medium, semibold, bold, display (Bricolage ExtraBold), displaySemi
function uiFont(kind as string, size as integer) as object
    if m.fonts = invalid then m.fonts = {}
    key = kind + size.ToStr()
    if m.fonts.DoesExist(key) then return m.fonts[key]
    files = {regular: "jakarta_regular.ttf", medium: "jakarta_medium.ttf", semibold: "jakarta_semibold.ttf", bold: "jakarta_bold.ttf", display: "bricolage_extrabold.ttf", displaySemi: "bricolage_semibold.ttf"}
    f = CreateObject("roSGNode", "Font")
    f.uri = "pkg:/fonts/" + files[kind]
    f.size = size
    m.fonts[key] = f
    return f
end function

function uiLabel(parent as object, text as string, x as float, y as float, size as integer, color as string, kind = "regular" as string, width = 0 as float, lines = 1 as integer, align = "left" as string) as object
    l = parent.createChild("Label")
    l.font = uiFont(kind, size)
    l.color = color
    l.text = text
    l.translation = [x, y]
    if width > 0 then
        l.width = width
        l.horizAlign = align
    end if
    if lines > 1 then
        l.wrap = true
        l.maxLines = lines
        l.lineSpacing = Int(size * 0.28)
    else
        l.wrap = false
    end if
    l.ellipsizeOnBoundary = true
    return l
end function

function radiusFile(r as integer, outline as boolean) as string
    sizes = [8, 12, 16, 20, 24, 28]
    best = 8
    for each s in sizes
        if Abs(s - r) < Abs(best - r) then best = s
    end for
    if outline then return "pkg:/images/o" + best.ToStr() + ".9.png"
    return "pkg:/images/r" + best.ToStr() + ".9.png"
end function

function pillFile(h as integer, outline as boolean) as string
    sizes = [18, 22, 28, 33, 39, 42]
    best = 18
    for each s in sizes
        if Abs(s * 2 - h) < Abs(best * 2 - h) then best = s
    end for
    if outline then return "pkg:/images/po" + best.ToStr() + ".9.png"
    return "pkg:/images/p" + best.ToStr() + ".9.png"
end function

' Caja con esquinas redondeadas del color indicado (radius 0 = rectángulo).
function uiBox(parent as object, x as float, y as float, w as float, h as float, color as string, radius = 16 as integer) as object
    if radius <= 0 then
        r = parent.createChild("Rectangle")
        r.color = color
        r.width = w
        r.height = h
        r.translation = [x, y]
        return r
    end if
    p = parent.createChild("Poster")
    p.uri = radiusFile(radius, false)
    p.blendColor = color
    p.width = w
    p.height = h
    p.translation = [x, y]
    return p
end function

function uiPill(parent as object, x as float, y as float, w as float, h as float, color as string) as object
    p = parent.createChild("Poster")
    p.uri = pillFile(h, false)
    p.blendColor = color
    p.width = w
    p.height = h
    p.translation = [x, y]
    return p
end function

' Anillo de foco: contorno blanco separado 6 px del elemento.
function uiRing(parent as object, x as float, y as float, w as float, h as float, radius = 16 as integer, pill = false as boolean) as object
    p = parent.createChild("Poster")
    if pill then
        p.uri = pillFile(h + 12, true)
    else
        p.uri = radiusFile(radius + 6, true)
    end if
    p.blendColor = "0xFFFFFFFF"
    p.width = w + 12
    p.height = h + 12
    p.translation = [x - 6, y - 6]
    p.visible = false
    return p
end function

function uiIcon(parent as object, name as string, x as float, y as float, size as float, color = "0xF5F7FFFF" as string) as object
    p = parent.createChild("Poster")
    p.uri = "pkg:/images/icons/" + name + ".png"
    p.width = size
    p.height = size
    p.blendColor = color
    p.translation = [x, y]
    return p
end function

function uiImage(parent as object, uri as string, x as float, y as float, w as float, h as float, mode = "scaleToFit" as string) as object
    p = parent.createChild("Poster")
    p.loadDisplayMode = mode
    p.width = w
    p.height = h
    p.loadWidth = w
    p.loadHeight = h
    p.translation = [x, y]
    p.uri = uri
    return p
end function

function uiCircle(parent as object, x as float, y as float, size as float, color as string) as object
    p = parent.createChild("Poster")
    p.uri = "pkg:/images/circle128.png"
    p.blendColor = color
    p.width = size
    p.height = size
    p.translation = [x, y]
    return p
end function

function uiAvatar(parent as object, key as string, x as float, y as float, size as float) as object
    p = parent.createChild("Poster")
    p.uri = avatarUri(key)
    p.width = size
    p.height = size
    p.loadWidth = 300
    p.loadHeight = 300
    p.translation = [x, y]
    return p
end function

function avatarKeys() as object
    return ["nina", "nino", "chava", "chavo", "mama", "papa", "abuela", "abuelo", "perrito", "gatito", "ajolote", "xolo", "jaguar"]
end function

function avatarKey(k as dynamic) as string
    if k = invalid then return "nina"
    legacy = {audifonos: "chava", pelo_largo: "mama", gorra: "chavo", chongo: "abuela", coletas: "nina"}
    for each a in avatarKeys()
        if a = k then return k
    end for
    if legacy.DoesExist(k) then return legacy[k]
    return "nina"
end function

function avatarUri(k as dynamic) as string
    return "pkg:/images/avatars/" + avatarKey(k) + ".png"
end function

function uiSpinner(parent as object, x as float, y as float, size as float, color = "0x3DE0BEFF" as string) as object
    s = parent.createChild("BusySpinner")
    s.translation = [x, y]
    s.poster.uri = "pkg:/images/spinner.png"
    s.poster.width = size
    s.poster.height = size
    s.poster.blendColor = color
    s.spinInterval = 1.0
    s.control = "start"
    return s
end function

function uiClear(node as object)
    if node = invalid then return invalid
    node.removeChildrenIndex(node.getChildCount(), 0)
    return invalid
end function

' Logo de canal con placa; si no trae logo (o falla), iniciales sobre color.
function uiLogoPlate(parent as object, name as string, logo as string, x as float, y as float, w as float, h as float, radius = 12 as integer) as object
    g = parent.createChild("Group")
    g.translation = [x, y]
    if logo <> "" then
        uiBox(g, 0, 0, w, h, "0xFFFFFF30", radius)
        uiImage(g, logo, 6, 4, w - 12, h - 8)
    else
        uiBox(g, 0, 0, w, h, tileFor(name), radius)
        uiLabel(g, initials(name), 0, h / 2 - h * 0.22, Int(h * 0.36), "0xFFFFFFFF", "bold", w, 1, "center")
    end if
    return g
end function

function scene() as object
    return m.top.getScene()
end function

function app(fn as string, p = invalid as dynamic) as dynamic
    if p = invalid then p = {}
    return m.top.getScene().callFunc(fn, p)
end function

sub sfx(name as string)
    m.top.getScene().callFunc("playSfx", {name: name})
end sub

' ------------------------------------------------------------------ botones
' Devuelve { node, bg, ring, label, w, h, style }. style: primary | secondary | ghost
function uiButton(parent as object, x as float, y as float, text as string, icon = "" as string, style = "primary" as string, minW = 0 as float, h = 72 as float) as object
    g = parent.createChild("Group")
    g.translation = [x, y]
    fsize = Int(h * 0.4)
    probe = g.createChild("Label")
    probe.font = uiFont("bold", fsize)
    probe.text = text
    tw = probe.boundingRect().width
    g.removeChild(probe)
    iconW = 0
    if icon <> "" then iconW = Int(h * 0.42) + 14
    if text = "" then iconW = Int(h * 0.42)
    w = tw + iconW + Int(h * 0.9)
    if text = "" then w = h
    if w < minW then w = minW
    ring = g.createChild("Poster")
    ring.uri = pillFile(h + 12, true)
    ring.width = w + 12
    ring.height = h + 12
    ring.translation = [-6, -6]
    ring.blendColor = "0xFFFFFFFF"
    ring.visible = false
    bg = uiPill(g, 0, 0, w, h, "0x2350E0FF")
    cx = (w - tw - iconW) / 2
    ic = invalid
    if icon <> "" then ic = uiIcon(g, icon, cx, (h - Int(h * 0.42)) / 2, Int(h * 0.42), "0xFFFFFFFF")
    lb = uiLabel(g, text, cx + iconW, (h - fsize * 1.3) / 2, fsize, "0xFFFFFFFF", "bold")
    b = {node: g, bg: bg, ring: ring, label: lb, icon: ic, w: w, h: h, style: style}
    uiButtonFocus(b, false)
    return b
end function

sub uiButtonFocus(b as object, focused as boolean)
    b.ring.visible = focused
    if b.style = "primary" then
        b.bg.blendColor = "0x2350E0FF"
        if focused then b.bg.blendColor = "0x3563F0FF"
        b.label.color = "0xFFFFFFFF"
    else if b.style = "secondary" then
        b.bg.blendColor = "0x12235FFF"
        if focused then b.bg.blendColor = "0x1A2E78FF"
        b.label.color = "0xC8D2F5FF"
        if focused then b.label.color = "0xFFFFFFFF"
    else
        b.bg.blendColor = "0x00000000"
        if focused then b.bg.blendColor = "0x1A2E78FF"
        b.label.color = "0xC8D2F5FF"
    end if
    if b.icon <> invalid then b.icon.blendColor = b.label.color
end sub

' Teclado de la tele. callback: nombre de la función que recibe el texto (o invalid si canceló).
sub uiKeyboard(title as string, text as string, callback as string, secure = false as boolean)
    di = CreateObject("roDeviceInfo")
    ver = di.GetOSVersion()
    major = Val(ver.major)
    if major >= 10 then
        d = CreateObject("roSGNode", "StandardKeyboardDialog")
        d.title = title
        d.text = text
        d.buttons = ["Listo", "Cancelar"]
        if secure then
            if d.hasField("secureMode") then
                d.secureMode = true
            else if d.hasField("textEditBox") and d.textEditBox <> invalid then
                d.textEditBox.secureMode = true
            end if
        end if
    else
        d = CreateObject("roSGNode", "KeyboardDialog")
        d.title = title
        d.text = text
        d.buttons = ["Listo", "Cancelar"]
        if secure and d.keyboard <> invalid and d.keyboard.textEditBox <> invalid then d.keyboard.textEditBox.secureMode = true
    end if
    m.kbCallback = callback
    m.kbDialog = d
    d.observeFieldScoped("buttonSelected", "uiOnKeyboardButton")
    d.observeFieldScoped("wasClosed", "uiOnKeyboardClosed")
    m.top.getScene().dialog = d
end sub

sub uiOnKeyboardButton(ev as object)
    d = m.kbDialog
    if d = invalid then return
    idx = ev.getData()
    txt = d.text
    m.kbDialog = invalid
    d.close = true
    if idx = 0 then
        cb = m.kbCallback
        m.kbCallback = invalid
        if cb <> invalid then
            m.kbText = txt
            ' llamada diferida para salir del observador
            m.top.callFunc("onKeyboardText", {text: txt, cb: cb})
        end if
    end if
end sub

sub uiOnKeyboardClosed(ev as object)
    m.kbDialog = invalid
end sub
