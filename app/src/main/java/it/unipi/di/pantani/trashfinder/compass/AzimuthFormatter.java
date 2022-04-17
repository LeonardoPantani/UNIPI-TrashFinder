package it.unipi.di.pantani.trashfinder.compass;

import android.content.Context;

import it.unipi.di.pantani.trashfinder.R;


class AzimuthFormatter {
    private static final int[] sides = {0, 45, 90, 135, 180, 225, 270, 315, 360};
    private static String[] names = null;

    public AzimuthFormatter(Context context) {
        initLocalizedNames(context);
    }

    public String format(float azimuth) {
        int iAzimuth = (int) azimuth;
        int index = findClosestIndex(iAzimuth);
        return iAzimuth + "° " + names[index];
    }

    private void initLocalizedNames(Context context) {
        if (names == null) {
            names = new String[]{
                    context.getString(R.string.sotw_front),
                    context.getString(R.string.sotw_frontright),
                    context.getString(R.string.sotw_right),
                    context.getString(R.string.sotw_backright),
                    context.getString(R.string.sotw_back),
                    context.getString(R.string.sotw_backleft),
                    context.getString(R.string.sotw_left),
                    context.getString(R.string.sotw_frontleft),
                    context.getString(R.string.sotw_front)
            };
        }
    }

    /**
     * Finds index of the closest element to identify Side Of The World label
     *
     * @return index of the closest element
     */
    private static int findClosestIndex(int target) {
        // in the original binary search https://www.geeksforgeeks.org/find-closest-number-array/
        // you will see more steps to reduce the time
        // in in this particular case the corner conditions are never true
        // e.g. azimuth is never negative, so there is no point to check
        // these conditions. Also we don't check if target is equal to element of array,
        // because most of the time it's not.

        // and the main difference is it finds the index, not the value

        // Doing binary search
        int i = 0, j = sides.length, mid = 0;
        while (i < j) {
            mid = (i + j) / 2;

            /* If target is less than array element,
               then search in left */
            if (target < sides[mid]) {

                // If target is greater than previous
                // to mid, return closest of two
                if (mid > 0 && target > sides[mid - 1]) {
                    return getClosest(mid - 1, mid, target);
                }

                /* Repeat for left half */
                j = mid;
            } else {
                if (mid < sides.length - 1 && target < sides[mid + 1]) {
                    return getClosest(mid, mid + 1, target);
                }
                i = mid + 1; // update i
            }
        }

        // Only single element left after search
        return mid;
    }

    // Method to compare which one is the more close
    // We find the closest by taking the difference
    // between the target and both values. It assumes
    // that val2 is greater than val1 and target lies
    // between these two.
    private static int getClosest(int index1, int index2, int target) {
        if (target - sides[index1] >= sides[index2] - target) {
            return index2;
        }
        return index1;
    }
}
