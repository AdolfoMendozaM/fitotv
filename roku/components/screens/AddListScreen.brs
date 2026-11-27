' Agregar una lista: con el celular (QR) o aquí con el control.
sub init()
    m.top.observeField("active", "onActiveChange")
    m.c = palette()
    uiBox(m.top, 0, 0, 1920, 1080, m.c.noche, 0)
    m.g = m.top.createChild("Group")
    m.overlay = m.top.createChild("Group")
    m.formTab = 0
    m.vals = {name: "", url: "", server: "", user: "", pass: ""}
    m.focus = 0
    m.waiting = false
    m.formError = ""
    m.address = ""
    m.doneTimer = m.top.createChild("Timer")
    m.doneTimer.duration = 1.6
    m.doneTimer.observeField("fire", "afterAdded")
end sub

sub onActiveChange()
    if m.top.active then
        m.firstTime = m.top.params.firstTime = true
        scene().observeFieldScoped("listStatus", "onStatus")
        startServer()
        render()
        m.top.setFocus(true)
    else
        scene().unobserveFieldScoped("listStatus")
        stopServer()
    end if
end sub

' ------------------------------------------------------------------ servidor del celular
sub startServer()
    if m.server <> invalid then return
    m.server = CreateObject("roSGNode", "PairServer")
    m.server.observeField("address", "onAddress")
    m.server.observeField("received", "onReceived")
    m.server.observeField("state", "onAddress")
    m.server.control = "run"
end sub

sub stopServer()
    if m.server = invalid then return
    m.server.unobserveField("address")
    m.server.unobserveField("received")
    m.server.unobserveField("state")
    m.server.stopNow = true
    m.server = invalid
end sub

sub onAddress()
    m.address = m.server.address
    drawPhone()
end sub

sub onReceived()
    r = m.server.received
    if r = invalid or r.kind = invalid or m.waiting then return
    sfx("ok")
    name = r.name
    if name = invalid or name.Trim() = "" then name = "Mi lista"
    m.lastSent = r
    m.waiting = true
    if r.kind = "FILE" then
        app("addList", {kind: "FILE", name: name, text: r.text})
    else if r.kind = "XTREAM" then
        app("addList", {kind: "XTREAM", name: name, server: r.server, user: r.user, pass: r.pass})
    else
        app("addList", {kind: "URL", name: name, url: r.url})
    end if
end sub

' ------------------------------------------------------------------ dibujo
sub render()
    uiClear(m.g)
    c = m.c
    title = "Agrega una lista"
    if m.firstTime then title = "Agrega tu primera lista"
    uiLabel(m.g, title, 120, 84, 69, c.text, "display", 1300)
    uiLabel(m.g, "Elige cómo. Las dos formas terminan en el mismo lugar.", 120, 180, 30, c.muted, "regular", 1300)
    m.later = invalid
    if m.firstTime then
        m.later = uiButton(m.g, 0, 96, "Lo hago después", "", "ghost", 0, 64)
        m.later.node.translation = [1800 - m.later.w, 96]
    end if

    ' panel del celular
    p = m.g.createChild("Group")
    p.translation = [120, 250]
    uiBox(p, 0, 0, 820, 720, c.s1, 28)
    uiBox(p, 40, 40, 64, 64, c.menta, 18)
    uiIcon(p, "phone", 54, 54, 36, c.noche)
    uiLabel(p, "Con tu celular", 124, 38, 36, c.text, "display", 500)
    uiPill(p, 124, 84, 170, 36, "0x3DE0BE33")
    uiLabel(p, "Lo más fácil", 124, 89, 21, c.menta, "semibold", 170, 1, "center")
    m.phone = p.createChild("Group")
    drawPhone()

    ' panel del control
    q = m.g.createChild("Group")
    q.translation = [980, 250]
    uiBox(q, 0, 0, 820, 720, c.s1, 28)
    uiBox(q, 40, 40, 64, 64, c.s3, 18)
    uiIcon(q, "remote", 54, 54, 36, c.text)
    uiLabel(q, "Aquí, con el control", 124, 38, 36, c.text, "display", 640)
    uiLabel(q, "Con el teclado de la tele, tarda un poco más.", 124, 86, 23, c.muted, "regular", 660)
    m.remote = q.createChild("Group")
    if m.firstTime then uiLabel(m.g, "Después puedes agregar más listas desde Ajustes.", 980, 994, 22, c.dim, "regular", 820, 1, "center")
    drawRemote()
    paint()
end sub

sub drawPhone()
    if m.phone = invalid then return
    c = m.c
    uiClear(m.phone)
    if m.address = "" then
        if m.server <> invalid and m.server.state = "run" then
            uiSpinner(m.phone, 330, 260, 96, c.menta)
            uiLabel(m.phone, "Preparando el código…", 40, 380, 26, c.muted, "regular", 740, 1, "center")
        else
            uiIcon(m.phone, "warn", 370, 230, 80, c.ambar)
            uiLabel(m.phone, "No encontramos la red de la tele. Revisa que esté conectada al Wi-Fi.", 80, 340, 27, c.muted2, "regular", 660, 3, "center")
        end if
        return
    end if
    ' código QR sobre placa blanca
    qr = qrMatrix(m.address)
    n = qr.Count()
    plate = 372
    quiet = 4
    cell = Int(plate / (n + quiet * 2))
    size = cell * (n + quiet * 2)
    ox = 40
    oy = 156
    uiBox(m.phone, ox, oy, size, size, "0xFFFFFFFF", 20)
    q0 = ox + cell * quiet
    q1 = oy + cell * quiet
    for y = 0 to n - 1
        row = qr[y]
        x = 0
        while x < n
            if row[x] then
                x0 = x
                while x < n and row[x]
                    x = x + 1
                end while
                r = m.phone.createChild("Rectangle")
                r.color = "0x0A1747FF"
                r.translation = [q0 + x0 * cell, q1 + y * cell]
                r.width = (x - x0) * cell
                r.height = cell
            else
                x = x + 1
            end if
        end while
    end for
    tx = ox + size + 40
    tw = 820 - tx - 40
    uiLabel(m.phone, "Escanea el QR con la cámara de tu celular.", tx, oy + 4, 28, c.text, "semibold", tw, 3)
    uiLabel(m.phone, "Ahí pegas tu lista y aparece aquí sola.", tx, oy + 128, 25, c.muted, "regular", tw, 3)
    uiLabel(m.phone, "O abre esta dirección en el navegador:", 40, 570, 24, c.muted, "regular", 740)
    uiPill(m.phone, 40, 608, 740, 56, c.s3)
    uiLabel(m.phone, Mid(m.address, 8), 40, 619, 29, c.text, "bold", 740, 1, "center")
    uiLabel(m.phone, "Tu celular y la tele tienen que estar en la misma red Wi-Fi.", 40, 676, 21, c.dim, "regular", 740, 1, "center")
end sub

function fields() as object
    if m.formTab = 0 then
        return [
            {key: "name", label: "Nombre de la lista", hint: "Mi lista"},
            {key: "url", label: "Enlace", hint: "http://…/lista.m3u"}
        ]
    end if
    return [
        {key: "name", label: "Nombre de la lista", hint: "Mi lista"},
        {key: "server", label: "Servidor", hint: "http://servidor.com:8080"},
        {key: "user", label: "Usuario", hint: ""},
        {key: "pass", label: "Contraseña", hint: ""}
    ]
end function

sub drawRemote()
    c = m.c
    uiClear(m.remote)
    ' pestañas
    t = m.remote.createChild("Group")
    t.translation = [40, 150]
    m.tabRing = uiRing(t, 0, 0, 740, 64, 0, true)
    uiPill(t, 0, 0, 740, 64, c.noche)
    m.tabBg = []
    m.tabLb = []
    names = ["Enlace", "Usuario y contraseña"]
    for i = 0 to 1
        bg = uiPill(t, 6 + i * 364, 6, 364, 52, c.clear)
        lb = uiLabel(t, names[i], 6 + i * 364, 17, 23, c.muted2, "semibold", 364, 1, "center")
        m.tabBg.Push(bg)
        m.tabLb.Push(lb)
    end for
    ' campos
    m.fieldNodes = []
    flist = fields()
    y = 236
    stepY = 96
    if flist.Count() > 2 then stepY = 84
    for each f in flist
        g = m.remote.createChild("Group")
        g.translation = [40, y]
        ring = uiRing(g, 0, 0, 740, 74, 18)
        bg = uiBox(g, 0, 0, 740, 74, c.s2, 18)
        uiLabel(g, f.label, 24, 10, 20, c.dim, "medium", 690)
        v = m.vals[f.key]
        shown = v
        col = c.text
        if f.key = "pass" and v <> "" then shown = String(Len(v), "•")
        if v = "" then
            shown = f.hint
            col = "0x5C6FB0FF"
        end if
        uiLabel(g, shown, 24, 34, 26, col, "semibold", 690)
        m.fieldNodes.Push({ring: ring, bg: bg, key: f.key, label: f.label})
        y = y + stepY
    end for
    m.err = uiLabel(m.remote, m.formError, 40, y + 2, 22, c.ambar, "medium", 740)
    m.addBtn = uiButton(m.remote, 40, 616, "Agregar lista", "plus", "primary", 740, 72)
end sub

' Orden del foco: [después], pestañas, campos..., botón
function focusItems() as object
    items = []
    if m.later <> invalid then items.Push("later")
    items.Push("tabs")
    for i = 0 to m.fieldNodes.Count() - 1
        items.Push("f" + i.ToStr())
    end for
    items.Push("add")
    return items
end function

sub paint()
    items = focusItems()
    if m.focus >= items.Count() then m.focus = items.Count() - 1
    cur = items[m.focus]
    c = m.c
    if m.later <> invalid then uiButtonFocus(m.later, cur = "later")
    m.tabRing.visible = cur = "tabs"
    for i = 0 to 1
        if i = m.formTab then
            m.tabBg[i].blendColor = c.azul
            m.tabLb[i].color = c.white
        else
            m.tabBg[i].blendColor = c.clear
            m.tabLb[i].color = c.muted2
        end if
    end for
    for i = 0 to m.fieldNodes.Count() - 1
        f = cur = "f" + i.ToStr()
        m.fieldNodes[i].ring.visible = f
        if f then m.fieldNodes[i].bg.blendColor = c.s3 else m.fieldNodes[i].bg.blendColor = c.s2
    end for
    uiButtonFocus(m.addBtn, cur = "add")
end sub

sub submit()
    name = m.vals.name.Trim()
    if name = "" then name = "Mi lista"
    if m.formTab = 0 then
        u = m.vals.url.Trim()
        if u = "" then
            m.formError = "Falta el enlace de la lista."
            sfx("off")
            drawRemote()
            paint()
            return
        end if
        if not looksLikeUrl(u) then u = "http://" + u
        m.lastSent = invalid
        m.waiting = true
        app("addList", {kind: "URL", name: name, url: u})
    else
        if m.vals.server.Trim() = "" or m.vals.user.Trim() = "" or m.vals.pass = "" then
            m.formError = "Faltan datos: servidor, usuario y contraseña."
            sfx("off")
            drawRemote()
            paint()
            return
        end if
        m.lastSent = invalid
        m.waiting = true
        app("addList", {kind: "XTREAM", name: name, server: normalizeServer(m.vals.server), user: m.vals.user.Trim(), pass: m.vals.pass})
    end if
    sfx("ok")
    m.formError = ""
end sub

' ------------------------------------------------------------------ estado
sub onStatus()
    if not m.waiting then return
    s = scene().listStatus
    if s = invalid then return
    c = m.c
    uiClear(m.overlay)
    m.overlayBtn = invalid
    if s.state = "checking" then
        drawOverlayBase()
        uiSpinner(m.ov, 352, 40, 96, c.menta)
        uiLabel(m.ov, "Revisando tu lista…", 0, 170, 48, c.text, "display", 800, 1, "center")
        uiLabel(m.ov, "Las listas grandes tardan unos segundos.", 0, 240, 27, c.muted, "regular", 800, 1, "center")
    else if s.state = "added" then
        m.waiting = false
        sfx("success")
        drawOverlayBase()
        uiCircle(m.ov, 352, 30, 96, c.menta)
        uiIcon(m.ov, "check", 376, 54, 48, c.noche)
        uiLabel(m.ov, "¡Listo!", 0, 160, 60, c.text, "display", 800, 1, "center")
        uiLabel(m.ov, s.name + " trae " + s.count.ToStr() + " canales y programas.", 0, 244, 28, c.muted2, "regular", 800, 2, "center")
        m.doneTimer.control = "start"
    else if s.state = "error" then
        m.waiting = false
        sfx("off")
        drawOverlayBase()
        uiCircle(m.ov, 352, 30, 96, "0xFFC53D33")
        uiIcon(m.ov, "warn", 376, 54, 48, c.ambar)
        uiLabel(m.ov, "No pudimos abrir la lista", 0, 160, 48, c.text, "display", 800, 1, "center")
        uiLabel(m.ov, s.message, 0, 228, 27, c.muted2, "regular", 800, 3, "center")
        m.overlayBtn = uiButton(m.ov, 0, 340, "Intentar otra vez", "refresh", "primary")
        m.overlayBtn.node.translation = [(800 - m.overlayBtn.w) / 2, 340]
        uiButtonFocus(m.overlayBtn, true)
    end if
end sub

sub drawOverlayBase()
    uiBox(m.overlay, 0, 0, 1920, 1080, m.c.scrim, 0)
    m.ov = m.overlay.createChild("Group")
    m.ov.translation = [560, 330]
end sub

sub afterAdded()
    uiClear(m.overlay)
    m.vals = {name: "", url: "", server: "", user: "", pass: ""}
    if m.firstTime then
        app("reset", {screen: "HomeScreen"})
    else
        app("goTab", {name: "inicio"})
    end if
end sub

' ------------------------------------------------------------------ teclas
function onKeyEvent(key as string, press as boolean) as boolean
    if not press then return false
    if m.overlay.getChildCount() > 0 then
        if m.overlayBtn <> invalid and (key = "OK" or key = "back") then
            sfx("back")
            uiClear(m.overlay)
            m.overlayBtn = invalid
            if key = "OK" and m.lastSent <> invalid then
                onReceivedAgain()
            end if
            return true
        end if
        return true
    end if
    items = focusItems()
    cur = items[m.focus]
    if key = "up" then
        if m.focus > 0 then
            m.focus = m.focus - 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "down" then
        if m.focus < items.Count() - 1 then
            m.focus = m.focus + 1
            sfx("tick")
            paint()
        end if
        return true
    else if key = "left" or key = "right" then
        if cur = "tabs" then
            nt = 0
            if key = "right" then nt = 1
            if nt <> m.formTab then
                m.formTab = nt
                m.formError = ""
                sfx("tick")
                drawRemote()
                paint()
            end if
        end if
        return true
    else if key = "OK" then
        if cur = "later" then
            sfx("ok")
            app("reset", {screen: "HomeScreen"})
        else if cur = "tabs" then
            m.formTab = 1 - m.formTab
            sfx("tick")
            drawRemote()
            paint()
        else if cur = "add" then
            submit()
        else
            i = Val(Mid(cur, 2))
            f = m.fieldNodes[i]
            sfx("ok")
            m.editKey = f.key
            uiKeyboard(f.label, m.vals[f.key], "field", f.key = "pass")
        end if
        return true
    else if key = "back" then
        if m.firstTime then
            ' en el primer arranque, Atrás no saca de la app sin preguntar
            sfx("back")
            app("reset", {screen: "HomeScreen"})
            return true
        end if
    end if
    return false
end function

' Reintenta lo último que llegó del celular.
sub onReceivedAgain()
    r = m.lastSent
    m.waiting = true
    if r.kind = "FILE" then
        app("addList", {kind: "FILE", name: r.name, text: r.text})
    else if r.kind = "XTREAM" then
        app("addList", {kind: "XTREAM", name: r.name, server: r.server, user: r.user, pass: r.pass})
    else
        app("addList", {kind: "URL", name: r.name, url: r.url})
    end if
end sub

function onKeyboardText(p as object) as dynamic
    if m.editKey <> invalid then
        v = p.text
        if m.editKey <> "pass" then v = v.Trim()
        m.vals[m.editKey] = v
        m.formError = ""
        drawRemote()
        paint()
    end if
    m.top.setFocus(true)
    return true
end function
