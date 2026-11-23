import { Injectable } from '@angular/core';
import { Router, CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../models/user.model';

/**
 * Protects authenticated areas.
 *
 * - Not logged in → redirect to /login (keeping the requested URL as returnUrl).
 * - Route declares `data: { roles: [...] }` and the user's role is not listed →
 *   redirect to /dashboard, which sends the user to their own area.
 *
 * This is a UX guard only: the API gateway and services remain the authority on access.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    if (!this.authService.isAuthenticated()) {
      return this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }

    const allowedRoles = route.data?.['roles'] as UserRole[] | undefined;
    if (!allowedRoles || allowedRoles.length === 0) {
      return true;
    }

    const role = this.authService.getCurrentUser()?.role;
    if (role && allowedRoles.includes(role)) {
      return true;
    }
    return this.router.createUrlTree(['/dashboard']);
  }
}
