import { Outlet } from "react-router-dom";
import { AppSidebar } from "./AppSidebar";
import { SidebarInset, SidebarProvider, SidebarTrigger } from "./ui/sidebar";

export function AppLayout() {
  return (
    <SidebarProvider className="bg-slate-950">
      <AppSidebar />
      <SidebarInset className="bg-slate-950 border-none">
        <header className="flex h-16 items-center px-6 md:hidden border-b border-slate-900">
          <SidebarTrigger className="text-white" />
        </header>
        <main className="flex-1 p-6 lg:p-10 overflow-y-auto">
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  );
}
