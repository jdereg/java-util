import ncube.grv.exp.cdn.CdnDefaultHandler

/**
 * This class acts like a dynamic cache.  If the item is found, then the value is returned.
 * If the item is not found, it is added to the Axis, and then a recursive getCell() call is
 * made, and that value is then returned.
 *
 * As a result, the Axis Columns grow each time an item is not found.
 */
class CdnDefaultRouter extends CdnDefaultHandler
{
    def run()
    {
        resolve('html')
    }
}