/* TODO: separate filtered store for old orders
   var oldOrdersFilter = new Ext.util.Filter({
    filterFn: function(item) {
        return item.submissionDate != null
    }
})*/

Ext.define("MobiPrint.view.OrderDetail", {
    extend: "Ext.Panel",
    xtype: "mobiprint-orderdetail",
    config: {
        layout: "fit",
        // Seems like this can't be changed later for the "current order" view, probably because it's the first view
        // in the NavigationView and it is never popped.
        title: _("CURRENT_ORDER"),

        scrollable: false,

        items: {
            xtype: "label",
            id: "order-detail-label",
            docked: "top"
        },
    },

    destroy: function() {
        this.callParent()

        // Avoid memory leak used by reference to method
        Ext.Viewport.removeListener("orientationchange", this.redraw, this)
    },

    initialize: function() {
        Ext.Viewport.addListener("orientationchange", this.redraw, this)
    },

    redraw: function() {
        var order = this.data.order
        var uploadingPictures
        var numUploadingPictures = 0
        var isOldOrder = order.submissionDate
        var label = this.down("#order-detail-label")

        if(!isOldOrder)
        {
            uploadingPictures = []

            for(var key in this.data.uploadingPictures)
            {
                uploadingPictures.push(key)
                ++numUploadingPictures
            }
        }

        label.setHtml(Ext.htmlEncode(Ext.String.format(_("ORDER_CONTAINS_N_PICTURES_FMT").toString(),
                                                       order.pictureIds.length + numUploadingPictures)))

        this.setTitle(Ext.htmlEncode(isOldOrder ? _("OLD_ORDER") : _("CURRENT_ORDER")))

        var gridWidth = Math.floor(window.innerWidth / 90)
        var itemWidth = Math.floor((window.innerWidth - 10) / gridWidth) - 10
        var maxItemHeight = itemWidth

        // Remove old panel if any
        var oldPanel = this.down("panel")
        if(oldPanel)
            oldPanel.destroy()

        var panel = Ext.create("Ext.Panel", {
            layout: "vbox",
            align: "stretch",
            scrollable: true
        })
        var container, i

        for(i = 0; i < order.pictureIds.length + numUploadingPictures; ++i)
        {
            if(i % gridWidth == 0)
            {
                container = Ext.create("Ext.Container", {
                    height: maxItemHeight,
                    layout: "hbox"
                })
                panel.add(container)
            }

            var pictureId = i < order.pictureIds.length ? order.pictureIds[i] : null
            var uploadingFilename = i < order.pictureIds.length ? null : uploadingPictures[i - order.pictureIds.length]
            var imageSrc
            var status, statusText
            var statusText
            if(isOldOrder)
                status = "printed"
            else
                status = pictureId == null ? "uploading" : "uploaded"

            if(pictureId)
                imageSrc = WEB_SERVICE_BASE_URI + "picture/" + order.pictureIds[i] + "/thumbnail/?size=300"
            else
                imageSrc = uploadingFilename

            statusText = _(status.toUpperCase()) // "UPLOADED", "UPLOADING", "PRINTED"

            container.add({
                xtype: "container",
                align: "stretch",
                pack: "start",
                layout: "vbox",
                flex: 1,
                items: [{
                    xtype: "panel",
                    layout: "fit",
                    html: ("<img src='" +
                           Ext.htmlEncode(imageSrc) +
                           "' class='order-detail-thumbnail' />")
                }, {
                    xtype: "panel",
                    html: "<p class='order-detail-status'><img src='resources/images/" + status + ".png'/> <span>" + Ext.htmlEncode(statusText) + "</span></p>"
                }]
            })
        }

        // Make grid layout equal
        if(i % gridWidth != 0)
            for(var n = 0; n < gridWidth - (i % gridWidth); ++n)
                container.add({
                    xtype: "panel",
                    flex: 1
                })

        this.add(panel)
    },

    updateData: function(newData) {
        // Only change view if something changed in the order or uploading pictures. Note that it might still in
        // the current order view because Ext.JSON.encode does not sort the keys and thus may produce different JSON
        // for equal dictionaries.
        if(this.data && Ext.JSON.encode(this.data) == Ext.JSON.encode(newData))
            return

        this.data = newData
        this.redraw()
    }
})
