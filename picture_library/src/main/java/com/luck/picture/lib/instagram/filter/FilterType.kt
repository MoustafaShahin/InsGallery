package com.luck.picture.lib.instagram.filter

import android.content.*
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import java.util.*

/**
 * ================================================
 * Created by JessYan on 2020/6/2 16:17
 * [Contact me](mailto:jess.yan.effort@gmail.com)
 * [Follow me](https://github.com/JessYanCoding)
 * ================================================
 */
enum class FilterType(private override val name: String) {
    I_NORMAL("Normal"), I_1977("1977"), I_AMARO("Amaro"), I_BRANNAN("Brannan"), I_EARLYBIRD("Earlybird"), I_HEFE("Hefe"), I_HUDSON("Hudson"), I_INKWELL("Inkwell"), I_LOMO("Lomo"), I_LORDKELVIN("LordKelvin"), I_NASHVILLE("Nashville"), I_RISE("Rise"), I_SIERRA("Sierra"), I_SUTRO("Sutro"), I_TOASTER("Toaster"), I_VALENCIA("Valencia"), I_WALDEN("Walden"), I_XPROII("X-Pro II");

    fun getName(): String {
        return name
    }

    companion object {
        fun createFilterList(): List<FilterType> {
            return Arrays.asList(*values())
        }

        fun createImageFilterList(context: Context): List<GPUImageFilter> {
            val imageFilters: MutableList<GPUImageFilter> = ArrayList()
            for (filterType in values()) {
                imageFilters.add(createFilterForType(context, filterType))
            }
            return imageFilters
        }

        fun createFilterForType(context: Context, type: FilterType?): GPUImageFilter {
            return when (type) {
                I_NORMAL -> GPUImageFilter()
                I_1977 -> IF1977Filter(context)
                I_AMARO -> IFAmaroFilter(context)
                I_BRANNAN -> IFBrannanFilter(context)
                I_EARLYBIRD -> IFEarlybirdFilter(context)
                I_HEFE -> IFHefeFilter(context)
                I_HUDSON -> IFHudsonFilter(context)
                I_INKWELL -> IFInkwellFilter(context)
                I_LOMO -> IFLomoFilter(context)
                I_LORDKELVIN -> IFLordKelvinFilter(context)
                I_NASHVILLE -> IFNashvilleFilter(context)
                I_RISE -> IFRiseFilter(context)
                I_SIERRA -> IFSierraFilter(context)
                I_SUTRO -> IFSutroFilter(context)
                I_TOASTER -> IFToasterFilter(context)
                I_VALENCIA -> IFValenciaFilter(context)
                I_WALDEN -> IFWaldenFilter(context)
                I_XPROII -> IFXprollFilter(context)
                else -> throw IllegalStateException("No filter of that type!")
            }
        }
    }
}