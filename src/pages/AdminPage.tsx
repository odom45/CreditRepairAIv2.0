import { useState } from "react";
import { ShieldAlert, Users, Settings, Database, AlertTriangle, CheckCircle2, MessageSquare, Megaphone, TicketPercent, RefreshCcw, Calendar } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";

const users = [
  { id: 1, name: "Benjamin Odom", email: "benjaminjodom45@gmail.com", status: "active", plan: "unlimited" },
  { id: 2, name: "Test User", email: "test@example.com", status: "active", plan: "free" },
  { id: 3, name: "John Doe", email: "john@doe.com", status: "inactive", plan: "pro" }
];

export function AdminPage() {
  const [announcement, setAnnouncement] = useState("");

  const handleAction = (action: string) => {
    setTimeout(() => {
      toast.success(`Admin action: ${action} completed!`);
    }, 1000);
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 pb-20">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
          <ShieldAlert className="size-8 text-primary" />
          Administrator Dashboard
        </h1>
        <p className="text-muted-foreground">Manage users, system health, and administrative actions.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">TOTAL USERS</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">1,248</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">ACTIVE DISPUTES</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-primary">3,892</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">SYSTEM HEALTH</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-emerald-500">99.9%</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">REVENUE (MTD)</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-chart-3">$12,450</div>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-8 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Users className="size-5 text-primary" />
              User Management
            </CardTitle>
            <CardDescription>Manage user accounts, subscriptions, and refunds.</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {users.map((user) => (
                <div key={user.id} className="flex flex-col p-4 rounded-lg border bg-card gap-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className="size-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold">
                        {user.name[0]}
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-medium">{user.name}</span>
                          <Badge variant="outline" className={
                            user.plan === "unlimited" ? "bg-primary/10 text-primary border-primary/20" : "bg-muted text-muted-foreground"
                          }>
                            {user.plan.toUpperCase()}
                          </Badge>
                        </div>
                        <p className="text-xs text-muted-foreground">{user.email}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button variant="ghost" size="sm">Edit</Button>
                      <Button variant="ghost" size="sm" className="text-destructive">Suspend</Button>
                    </div>
                  </div>
                  
                  <div className="flex flex-wrap gap-2 pt-2 border-t border-border/50">
                    <Button variant="outline" size="sm" className="h-8 gap-1" onClick={() => handleAction(`Discount for ${user.name}`)}>
                      <TicketPercent className="size-3" />
                      Apply Discount
                    </Button>
                    <Button variant="outline" size="sm" className="h-8 gap-1" onClick={() => handleAction(`Refund for ${user.name}`)}>
                      <RefreshCcw className="size-3" />
                      Issue Refund
                    </Button>
                    <Button variant="outline" size="sm" className="h-8 gap-1" onClick={() => handleAction(`Free Month for ${user.name}`)}>
                      <Calendar className="size-3" />
                      Give Free Month
                    </Button>
                    <Button variant="outline" size="sm" className="h-8 gap-1" onClick={() => handleAction(`Message to ${user.name}`)}>
                      <MessageSquare className="size-3" />
                      Respond
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <div className="space-y-8">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Megaphone className="size-5 text-primary" />
                Announcements
              </CardTitle>
              <CardDescription>Send updates to all users.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Textarea 
                placeholder="Type your update or announcement here..." 
                value={announcement}
                onChange={(e) => setAnnouncement(e.target.value)}
                className="min-h-[100px]"
              />
              <Button className="w-full gap-2" onClick={() => {
                handleAction("Global Announcement");
                setAnnouncement("");
              }}>
                <Megaphone className="size-4" />
                Post Announcement
              </Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <TicketPercent className="size-5 text-primary" />
                Global Discounts
              </CardTitle>
              <CardDescription>Apply to all app users.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2">
                <Input placeholder="Discount %" type="number" />
                <Button variant="outline" onClick={() => handleAction("Global Discount")}>Apply</Button>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Settings className="size-5 text-muted-foreground" />
                System Actions
              </CardTitle>
              <CardDescription>Global system controls.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <Button className="w-full justify-start gap-2" variant="outline" onClick={() => handleAction("Clear Cache")}>
                <Database className="size-4" />
                Clear System Cache
              </Button>
              <Button className="w-full justify-start gap-2" variant="outline" onClick={() => handleAction("Sync Data")}>
                <CheckCircle2 className="size-4" />
                Sync Bureau Data
              </Button>
              <Button className="w-full justify-start gap-2 text-destructive hover:bg-destructive/5" variant="outline" onClick={() => handleAction("Maintenance Mode")}>
                <AlertTriangle className="size-4" />
                Enable Maintenance Mode
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
