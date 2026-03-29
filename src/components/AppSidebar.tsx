import { useAuthActions } from "@convex-dev/auth/react";
import { useQuery } from "convex/react";
import { LayoutDashboard, LogOut, Moon, Settings, Sun, FileText, Upload, BarChart3, Globe, Shield, ShieldAlert, Zap } from "lucide-react";
import { Link, useLocation } from "react-router-dom";
import { useTheme } from "@/contexts/ThemeContext";
import { APP_NAME } from "@/lib/constants";
import { api } from "../../convex/_generated/api";
import { Avatar, AvatarFallback } from "./ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "./ui/sidebar";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/upload", label: "Upload Reports", icon: Upload },
  { href: "/analysis", label: "AI Analysis", icon: BarChart3 },
  { href: "/disputes", label: "Disputes", icon: FileText },
  { href: "/cfpb", label: "CFPB Assistant", icon: Globe },
  { href: "/freezes", label: "Bureau Freezes", icon: Shield },
];

function NavLink({
  href,
  label,
  icon: Icon,
  isActive,
}: {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  isActive: boolean;
}) {
  const { setOpenMobile } = useSidebar();

  return (
    <SidebarMenuItem>
      <SidebarMenuButton asChild isActive={isActive} className={`h-12 rounded-xl transition-all duration-300 ${isActive ? "bg-blue-600 text-white shadow-lg shadow-blue-600/20" : "text-slate-400 hover:bg-slate-800 hover:text-white"}`}>
        <Link to={href} onClick={() => setOpenMobile(false)} className="flex items-center gap-3 px-4">
          <Icon className={`size-5 ${isActive ? "text-white" : "text-slate-500 group-hover:text-blue-400"}`} />
          <span className="font-bold text-sm">{label}</span>
        </Link>
      </SidebarMenuButton>
    </SidebarMenuItem>
  );
}

function SidebarNav() {
  const location = useLocation();
  const user = useQuery(api.auth.currentUser);
  const isAdmin = user?.email === "benjaminjodom45@gmail.com" || user?.email === "agent@test.local";

  return (
    <SidebarContent className="bg-slate-950 border-r border-slate-900">
      <SidebarGroup>
        <SidebarGroupContent className="px-4 py-6">
          <SidebarMenu className="space-y-2">
            {navItems.map(item => (
              <NavLink
                key={item.href}
                href={item.href}
                label={item.label}
                icon={item.icon}
                isActive={location.pathname === item.href}
              />
            ))}
            {isAdmin && (
              <NavLink
                href="/admin"
                label="Admin Panel"
                icon={ShieldAlert}
                isActive={location.pathname === "/admin"}
              />
            )}
          </SidebarMenu>
        </SidebarGroupContent>
      </SidebarGroup>
    </SidebarContent>
  );
}

function SidebarUserMenu() {
  const user = useQuery(api.auth.currentUser);
  const { signOut } = useAuthActions();
  const { theme, toggleTheme, switchable } = useTheme();
  const { setOpenMobile } = useSidebar();

  return (
    <SidebarFooter className="bg-slate-950 border-t border-slate-900 p-4">
      <SidebarMenu>
        <SidebarMenuItem>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <SidebarMenuButton size="lg" className="h-16 rounded-2xl bg-slate-900/50 border border-slate-800 hover:bg-slate-800 transition-all">
                <Avatar className="size-10 rounded-xl border-2 border-blue-500/20 shadow-lg shadow-blue-500/10">
                  <AvatarFallback className="bg-blue-600 text-white text-sm font-black">
                    {user?.name?.charAt(0).toUpperCase() || "U"}
                  </AvatarFallback>
                </Avatar>
                <div className="flex flex-col items-start text-left ml-3">
                  <span className="text-sm font-black text-white truncate">
                    {user?.name || "User"}
                  </span>
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest truncate">
                    {user?.email}
                  </span>
                </div>
              </SidebarMenuButton>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              side="top"
              align="start"
              className="w-[--radix-dropdown-menu-trigger-width] bg-slate-900 border-slate-800 text-white rounded-2xl p-2 shadow-2xl"
            >
              <DropdownMenuItem asChild className="rounded-xl hover:bg-slate-800 focus:bg-slate-800 cursor-pointer py-3">
                <Link to="/settings" onClick={() => setOpenMobile(false)} className="flex items-center gap-3">
                  <Settings className="size-4 text-blue-400" />
                  <span className="font-bold text-sm">Settings</span>
                </Link>
              </DropdownMenuItem>
              {switchable && (
                <DropdownMenuItem onClick={toggleTheme} className="rounded-xl hover:bg-slate-800 focus:bg-slate-800 cursor-pointer py-3">
                  <div className="flex items-center gap-3">
                    {theme === "light" ? (
                      <Moon className="size-4 text-indigo-400" />
                    ) : (
                      <Sun className="size-4 text-amber-400" />
                    )}
                    <span className="font-bold text-sm">{theme === "light" ? "Dark mode" : "Light mode"}</span>
                  </div>
                </DropdownMenuItem>
              )}
              <DropdownMenuSeparator className="bg-slate-800 my-2" />
              <DropdownMenuItem
                onClick={() => signOut()}
                className="rounded-xl hover:bg-rose-500/10 focus:bg-rose-500/10 text-rose-400 cursor-pointer py-3"
              >
                <div className="flex items-center gap-3">
                  <LogOut className="size-4" />
                  <span className="font-bold text-sm">Sign out</span>
                </div>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarFooter>
  );
}

function SidebarHeaderContent() {
  const { setOpenMobile } = useSidebar();

  return (
    <SidebarHeader className="bg-slate-950 border-b border-slate-900 p-6">
      <Link
        to="/"
        onClick={() => setOpenMobile(false)}
        className="flex items-center gap-3 font-black text-xl text-white group"
      >
        <div className="size-10 rounded-xl bg-blue-600 flex items-center justify-center shadow-lg shadow-blue-600/20 group-hover:scale-110 transition-transform duration-300">
          <Zap className="size-6 fill-white" />
        </div>
        <span className="bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent">
          {APP_NAME}
        </span>
      </Link>
    </SidebarHeader>
  );
}

export function AppSidebar() {
  return (
    <Sidebar className="border-none">
      <SidebarHeaderContent />
      <SidebarNav />
      <SidebarUserMenu />
    </Sidebar>
  );
}
