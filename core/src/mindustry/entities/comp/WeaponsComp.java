package mindustry.entities.comp;

import arc.math.geom.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.distribution.Router;
import static mindustry.MdUtil.*;

@Component
abstract class WeaponsComp implements Teamc, Posc, Rotc, Velc, Statusc{
    @Import float x, y;
    @Import boolean disarmed;
    @Import UnitType type;

    /** weapon mount array, never null */
    @SyncLocal WeaponMount[] mounts = {};
    @ReadOnly transient boolean isRotate;
    @Mask("mdAimXMask()") @NoSerialize float aimX;
    @Mask("mdAimYMask()") @NoSerialize float aimY;
    boolean isShooting;

    public boolean mdReplaceNaN() { return false; }

    // Anuke holy shit this why is this so unstable.
    // Why the fuck spectator changes this behavior
    public float mdAimXMask() {
        if (mdReplaceNaN()) return Float.NaN;
        float f = xcapdist(aimX, aimY, x, y, isShooting ? mdRange() : 2f);
        // Log.info("x: " + (f - x));
        return blackBox(f);
    }
    public float mdAimYMask() {
        if (mdReplaceNaN()) return Float.NaN;
        float f = ycapdist(aimX, aimY, x, y, isShooting ? mdRange() : 2f);
        // Log.info("y: " + (f - y));
        return blackBox(f);
    }

    public float mdRange() {
        return 2f;
    }

    void setWeaponRotation(float rotation){
        for(WeaponMount mount : mounts){
            mount.rotation = rotation;
        }
    }

    void setupWeapons(UnitType def){
        mounts = new WeaponMount[def.weapons.size];
        for(int i = 0; i < mounts.length; i++){
            mounts[i] = def.weapons.get(i).mountType.get(def.weapons.get(i));
        }
    }

    void controlWeapons(boolean rotateShoot){
        controlWeapons(rotateShoot, rotateShoot);
    }

    void controlWeapons(boolean rotate, boolean shoot){
        for(WeaponMount mount : mounts){
            if(mount.weapon.controllable){
                mount.rotate = rotate;
                mount.shoot = shoot;
            }
        }
        isRotate = rotate;
        isShooting = shoot;
    }

    void aim(Position pos){
        aim(pos.getX(), pos.getY());
    }

    /** Aim at something. This will make all mounts point at it. */
    void aim(float x, float y){
        Tmp.v1.set(x, y).sub(this.x, this.y);
        if(Tmp.v1.len() < type.aimDst) Tmp.v1.setLength(type.aimDst);

        x = Tmp.v1.x + this.x;
        y = Tmp.v1.y + this.y;

        for(WeaponMount mount : mounts){
            if(mount.weapon.controllable){
                mount.aimX = x;
                mount.aimY = y;
            }
        }

        aimX = x;
        aimY = y;
    }

    boolean canShoot(){
        return !disarmed;
    }

    @Override
    public void remove(){
        for(WeaponMount mount : mounts){
            if(mount.weapon.continuous && mount.bullet != null && mount.bullet.owner == self()){
                mount.bullet.time = mount.bullet.lifetime - 10f;
                mount.bullet = null;
            }

            if(mount.sound != null){
                mount.sound.stop();
            }
        }
    }

    /** Update shooting and rotation for this unit. */
    @Override
    public void update(){
        for(WeaponMount mount : mounts){
            mount.weapon.update(self(), mount);
        }
    }
}
